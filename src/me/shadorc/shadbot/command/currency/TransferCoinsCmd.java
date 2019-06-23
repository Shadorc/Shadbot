package me.shadorc.shadbot.command.currency;

import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.data.database.DBMember;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.exception.CommandException;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.object.help.HelpBuilder;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.TextUtils;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TransferCoinsCmd extends BaseCmd {

    public TransferCoinsCmd() {
        super(CommandCategory.CURRENCY, List.of("transfer"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2);

        if (context.getMessage().getUserMentionIds().isEmpty()) {
            return Mono.error(new MissingArgumentException());
        }

        final Snowflake senderUserId = context.getAuthorId();
        final Snowflake receiverUserId = new ArrayList<>(context.getMessage().getUserMentionIds()).get(0);
        if (receiverUserId.equals(senderUserId)) {
            return Mono.error(new CommandException("You cannot transfer coins to yourself."));
        }

        final Integer coins = NumberUtils.asPositiveInt(args.get(0));
        if (coins == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid amount of coins.",
                    args.get(0))));
        }

        final DBMember dbSender = DatabaseManager.getInstance().getDBMember(context.getGuildId(), senderUserId);
        if (dbSender.getCoins() < coins) {
            return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
        }

        final DBMember dbReceiver = DatabaseManager.getInstance().getDBMember(context.getGuildId(), receiverUserId);
        if (dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
            return context.getClient().getUserById(receiverUserId)
                    .map(User::getUsername)
                    .flatMap(username -> context.getChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                    Emoji.BANK + " (**%s**) This transfer cannot be done because %s would exceed the maximum coins cap.",
                                    context.getUsername(), username), channel)))
                    .then();
        }

        dbSender.addCoins(-coins);
        dbReceiver.addCoins(coins);

        return context.getClient().getUserById(receiverUserId).map(User::getMention)
                .flatMap(receiverMention -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(
                                String.format(Emoji.BANK + " %s has transfered **%s** to %s",
                                        context.getAuthor().getMention(), FormatUtils.coins(coins), receiverMention), channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Transfer coins to the mentioned user.")
                .addArg("coins", false)
                .addArg("@user", false)
                .build();
    }
}
