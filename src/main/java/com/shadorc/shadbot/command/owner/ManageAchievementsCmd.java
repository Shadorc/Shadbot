package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.EnumUtils;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.http.client.ClientException;
import io.netty.handler.codec.http.HttpResponseStatus;
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

        final Action action = EnumUtils.parseEnum(Action.class, args.get(0),
                new CommandException(String.format("`%s` is not a valid action. %s",
                        args.get(0), FormatUtils.options(Action.class))));

        final Achievement achievement = EnumUtils.parseEnum(Achievement.class, args.get(1),
                new CommandException(String.format("`%s` is not a valid achievement. %s",
                        args.get(1), FormatUtils.options(Achievement.class))));

        final Long userId = NumberUtils.toPositiveLongOrNull(args.get(2));
        if (userId == null) {
            return Mono.error(new CommandException("Invalid user ID."));
        }
        return context.getClient()
                .getUserById(Snowflake.of(userId))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> Mono.empty())
                .flatMap(user -> DatabaseManager.getUsers().getDBUser(user.getId())
                        .flatMap(dbUser -> switch (action) {
                            case ADD -> dbUser.unlockAchievement(achievement);
                            case REMOVE -> dbUser.lockAchievement(achievement);
                        })
                        .then(context.getChannel())
                        .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " %s **%s** to **%s** done.",
                                FormatUtils.capitalizeEnum(action), achievement.getTitle(), user.getTag()), channel)))
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .then();
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .addArg("action", FormatUtils.options(Action.class), false)
                .addArg("achievement", FormatUtils.options(Achievement.class), false)
                .addArg("userId", false)
                .build();
    }
}
