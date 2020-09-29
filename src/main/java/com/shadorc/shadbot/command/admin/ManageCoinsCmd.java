package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class ManageCoinsCmd extends BaseCmd {

    private enum Action {
        ADD, REMOVE, RESET;
    }

    public ManageCoinsCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("manage_coins", "manage_coin"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2, 3);

        final Action action = EnumUtils.parseEnum(Action.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(0), FormatUtils.options(Action.class))));

        final Long coins = NumberUtils.toLongOrNull(args.get(1));
        if (coins == null && action != Action.RESET) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid amount of coins.",
                    args.get(1))));
        }

        if (coins != null && coins > Config.MAX_COINS) {
            return Mono.error(new CommandException(String.format("You cannot transfer more than %s.",
                    FormatUtils.coins(Config.MAX_COINS))));
        }

        return DiscordUtils.getMembersFrom(context.getMessage())
                .flatMap(member -> Mono.zip(Mono.just(member.getUsername()),
                        DatabaseManager.getGuilds().getDBMember(member.getGuildId(), member.getId())))
                .collectList()
                // List<Tuple<Username, DBMember>>
                .flatMap(members -> {
                    if (members.isEmpty()) {
                        return Mono.error(new CommandException("You must specify at least one user / role."));
                    }

                    final String mentionsStr = context.getMessage().mentionsEveryone()
                            ? "Everyone" : FormatUtils.format(members, Tuple2::getT1, ", ");
                    return switch (action) {
                        case ADD -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(dbMember -> dbMember.addCoins(coins))
                                .then(Mono.just(String.format(Emoji.MONEY_BAG + " **%s** received **%s**.",
                                        mentionsStr, FormatUtils.coins(coins))));
                        case REMOVE -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(dbMember -> dbMember.addCoins(-coins))
                                .then(Mono.just(String.format(Emoji.MONEY_BAG + " **%s** lost **%s**.",
                                        mentionsStr, FormatUtils.coins(coins))));
                        case RESET -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(DBMember::resetCoins)
                                .then(Mono.just(String.format(Emoji.MONEY_BAG + " **%s** lost all %s coins.",
                                        mentionsStr, members.size() == 1 ? "his" : "their")));
                    };
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Manage user(s) coins.")
                .addArg("action", FormatUtils.format(Action.class, " / "), false)
                .addArg("coins", "can be positive or negative", true)
                .addArg("@user(s)/@role(s)", false)
                .setExample(String.format("`%s%s add 150 @Shadbot`%n`%s%s reset @Shadbot`",
                        context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
                .build();
    }
}
