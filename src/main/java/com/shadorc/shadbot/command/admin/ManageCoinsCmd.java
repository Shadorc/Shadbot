package com.shadorc.shadbot.command.admin;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.ratelimiter.RateLimiter;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.exception.CommandException;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

public class ManageCoinsCmd extends BaseCmd {

    private enum Action {
        ADD, REMOVE, RESET;
    }

    public ManageCoinsCmd() {
        super(CommandCategory.ADMIN, CommandPermission.ADMIN, List.of("manage_coins", "manage-coins", "managecoins"));
        this.setRateLimiter(new RateLimiter(2, Duration.ofSeconds(3)));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(2, 3);

        final Action action = Utils.parseEnum(Action.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(0), FormatUtils.options(Action.class))));

        final Integer coins = NumberUtils.toIntOrNull(args.get(1));
        if (coins == null && action != Action.RESET) {
            return Mono.error(new CommandException(String.format("`%s` is not a valid amount of coins.",
                    args.get(1))));
        }

        return DiscordUtils.getMembersFrom(context.getMessage())
                .collectList()
                .map(members -> {
                    if (members.isEmpty()) {
                        throw new CommandException("You must specify at least one user / role.");
                    }

                    final String mentionsStr = context.getMessage().mentionsEveryone() ? "Everyone" : FormatUtils.format(members, User::getUsername, ", ");
                    switch (action) {
                        case ADD:
                            members.forEach(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).addCoins(coins));
                            return String.format(Emoji.MONEY_BAG + " **%s** received **%s**.", mentionsStr, FormatUtils.coins(coins));
                        case REMOVE:
                            members.forEach(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).addCoins(-coins));
                            return String.format(Emoji.MONEY_BAG + " **%s** lost **%s**.", mentionsStr, FormatUtils.coins(coins));
                        case RESET:
                            members.forEach(user -> DatabaseManager.getGuilds().getDBMember(context.getGuildId(), user.getId()).resetCoins());
                            return String.format(Emoji.MONEY_BAG + " **%s** lost all %s coins.", mentionsStr, members.size() == 1 ? "his" : "their");
                        default:
                            throw new CommandException(String.format("`%s` is not a valid action. %s",
                                    args.get(0), FormatUtils.options(Action.class)));
                    }
                })
                .flatMap(text -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(text, channel)))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Manage user(s) coins.")
                .addArg("action", FormatUtils.format(Action.class, " / "), false)
                .addArg("coins", "can be positive or negative", true)
                .addArg("@user(s)/@role(s)", false)
                .setExample(String.format("`%s%s add 150 @Shadbot`%n`%s%s reset @Shadbot`",
                        context.getPrefix(), this.getName(), context.getPrefix(), this.getName()))
                .build();
    }
}
