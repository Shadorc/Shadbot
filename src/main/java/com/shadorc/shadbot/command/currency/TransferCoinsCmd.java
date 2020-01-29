package com.shadorc.shadbot.command.currency;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
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

        final Long coins = NumberUtils.toPositiveLongOrNull(args.get(0));
        if (coins == null) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid amount of coins.",
                    args.get(0))));
        }

        if (coins > Config.MAX_COINS) {
            return Mono.error(new CommandException(String.format("You cannot transfer more than %s.",
                    FormatUtils.coins(Config.MAX_COINS))));
        }

        return DatabaseManager.getGuilds()
                .getDBMembers(context.getGuildId(), senderUserId, receiverUserId)
                .collectMap(DBMember::getId)
                .flatMap(dbMembers -> {
                    final DBMember dbSender = dbMembers.get(senderUserId);
                    if (dbSender.getCoins() < coins) {
                        return Mono.error(new CommandException(TextUtils.NOT_ENOUGH_COINS));
                    }

                    final DBMember dbReceiver = dbMembers.get(receiverUserId);
                    if (dbReceiver.getCoins() + coins >= Config.MAX_COINS) {
                        return context.getClient()
                                .getUserById(receiverUserId)
                                .map(User::getUsername)
                                .flatMap(username -> context.getChannel()
                                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                                                Emoji.BANK + " (**%s**) This transfer cannot be done because %s would " +
                                                        "exceed the maximum coins cap.",
                                                context.getUsername(), username), channel)))
                                .then();
                    }

                    return dbSender.addCoins(-coins)
                            .and(dbReceiver.addCoins(coins))
                            .then(context.getClient().getUserById(receiverUserId))
                            .map(User::getMention)
                            .flatMap(receiverMention -> context.getChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(
                                            String.format(Emoji.BANK + " %s has transferred **%s** to %s",
                                                    context.getAuthor().getMention(), FormatUtils.coins(coins),
                                                    receiverMention), channel)))
                            .then();
                });
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .setDescription("Transfer coins to the mentioned user.")
                .addArg("coins", false)
                .addArg("@user", false)
                .build();
    }
}
