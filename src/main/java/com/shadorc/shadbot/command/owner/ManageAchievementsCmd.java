package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.utils.*;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class ManageAchievementsCmd extends BaseCmd {

    private enum Action {
        ADD, REMOVE;
    }

    public ManageAchievementsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER,
                List.of("manage_achievements", "manage_achievement"));
    }

    @Override
    public Mono<Void> execute(Context context) {
        final List<String> args = context.requireArgs(3);

        final Action action = Utils.parseEnum(Action.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(0), FormatUtils.options(Action.class))));

        final Achievement achievement = Utils.parseEnum(Achievement.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid achievement. %s",
                        args.get(1), FormatUtils.options(Achievement.class))));

        final Long userId = NumberUtils.toPositiveLongOrNull(args.get(2));
        if (userId == null) {
            return Mono.error(new CommandException("Invalid user ID."));
        }
        return context.getClient()
                .getUserById(Snowflake.of(userId))
                .onErrorResume(ClientException.isStatusCode(403), err -> Mono.empty())
                .flatMap(user -> DatabaseManager.getUsers().getDBUser(user.getId())
                        .flatMap(dbUser -> {
                            switch (action) {
                                case ADD:
                                    return dbUser.unlockAchievement(achievement);
                                case REMOVE:
                                    return dbUser.lockAchievement(achievement);
                                default:
                                    return Mono.error(new IllegalStateException(String.format("Unknown action: %s", action)));
                            }
                        })
                        .then(context.getChannel())
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s **%s** to **%s** done.",
                                StringUtils.capitalizeEnum(action), achievement.getTitle(), user.getTag()), channel)))
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return HelpBuilder.create(this, context)
                .addArg("action", FormatUtils.options(Action.class), false)
                .addArg("achievement", FormatUtils.options(Achievement.class), false)
                .addArg("userId", false)
                .build();
    }
}
