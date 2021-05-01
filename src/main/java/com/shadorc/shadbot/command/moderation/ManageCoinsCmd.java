package com.shadorc.shadbot.command.moderation;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBMember;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Optional;

public class ManageCoinsCmd extends BaseCmd {

    private static final int MIN_COINS = 1;

    // TODO Improvement: Transform these actions as sub-commands
    private enum Action {
        ADD, REMOVE, RESET
    }

    public ManageCoinsCmd() {
        super(CommandCategory.MODERATION, CommandPermission.ADMIN,
                "manage_coins", "Manage users coins");

        this.addOption(option -> option.name("action")
                .description("Whether to add, remove or reset coins")
                .type(ApplicationCommandOptionType.STRING.getValue())
                .required(true)
                .choices(DiscordUtil.toOptions(Action.class)));
        this.addOption(option -> option.name("coins")
                .description("The amount of coins to add/remove")
                .type(ApplicationCommandOptionType.INTEGER.getValue())
                .required(false));
        this.addOption(option -> option.name("user")
                .description("The user")
                .type(ApplicationCommandOptionType.USER.getValue())
                .required(false));
        this.addOption(option -> option.name("role")
                .description("The role")
                .type(ApplicationCommandOptionType.ROLE.getValue())
                .required(false));
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Optional<Long> coinsOpt = context.getOptionAsLong("coins");
        final Mono<Member> user = context.getOptionAsMember("user");
        final Optional<Snowflake> roleIdOpt = context.getOptionAsSnowflake("role");

        if (action != Action.RESET) {
            if (coinsOpt.isEmpty()) {
                return Mono.error(new CommandException(context.localize("managecoins.exception.missing.coins")));
            }
            if (!NumberUtil.isBetween(coinsOpt.orElseThrow(), MIN_COINS, Config.MAX_COINS)) {
                return Mono.error(new CommandException(context.localize("managecoins.exception.coins.out.of.range")
                        .formatted(MIN_COINS, context.localize(Config.MAX_COINS))));
            }
        }

        // TODO Question: If the guild is very large, isn't this gonna break everything?
        return Mono.justOrEmpty(roleIdOpt)
                .flatMapMany(roleId -> context.getGuild()
                        .flatMapMany(Guild::requestMembers)
                        .filter(member -> member.getRoleIds().contains(roleId)))
                .mergeWith(user)
                .flatMap(member -> Mono.zip(
                        Mono.just(member.getUsername()),
                        DatabaseManager.getGuilds().getDBMember(member.getGuildId(), member.getId())))
                .collectList()
                // List<Tuple<Username, DBMember>>
                .flatMap(members -> {
                    if (members.isEmpty()) {
                        return Mono.error(new CommandException(context.localize("managecoins.exception.user.role")));
                    }

                    // TODO Question: Is it possible to mention @everyone as a role? If so, do not mention everyone
                    final String mentionsStr = FormatUtil.format(members, Tuple2::getT1, ", ");
                    return switch (action) {
                        case ADD -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(dbMember -> dbMember.addCoins(coinsOpt.orElseThrow()))
                                .then(context.reply(Emoji.MONEY_BAG, context.localize("managecoins.add")
                                        .formatted(mentionsStr, context.localize(coinsOpt.orElseThrow()))));
                        case REMOVE -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(dbMember -> dbMember.addCoins(-coinsOpt.orElseThrow()))
                                .then(context.reply(Emoji.MONEY_BAG, context.localize("managecoins.remove")
                                        .formatted(mentionsStr, context.localize(coinsOpt.orElseThrow()))));
                        case RESET -> Flux.fromIterable(members)
                                .map(Tuple2::getT2)
                                .flatMap(DBMember::resetCoins)
                                .then(context.reply(Emoji.MONEY_BAG, context.localize("managecoins.reset")
                                        .formatted(mentionsStr)));
                    };
                });
    }

}

