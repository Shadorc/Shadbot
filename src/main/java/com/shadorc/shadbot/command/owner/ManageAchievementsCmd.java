package com.shadorc.shadbot.command.owner;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.CommandPermission;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.EnumUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import discord4j.common.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.ApplicationCommandOptionType;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

public class ManageAchievementsCmd extends BaseCmd {

    private enum Action {
        ADD, REMOVE
    }

    public ManageAchievementsCmd() {
        super(CommandCategory.OWNER, CommandPermission.OWNER, "manage_achievements", "Manage user's achievements");

        this.addOption("action", FormatUtil.options(Action.class), true, ApplicationCommandOptionType.STRING);
        this.addOption("achievement", FormatUtil.options(Achievement.class), true, ApplicationCommandOptionType.STRING);
        this.addOption("user", "The user", true, ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String actionOpt = context.getOption("action").orElseThrow();
        final String achievementOpt = context.getOption("achievement").orElseThrow();
        final Snowflake userId = Snowflake.of(context.getOption("user").orElseThrow());

        final Action action = EnumUtil.parseEnum(Action.class, actionOpt,
                new CommandException(String.format("`%s` is not a valid action. %s",
                        actionOpt, FormatUtil.options(Action.class))));

        final Achievement achievement = EnumUtil.parseEnum(Achievement.class, achievementOpt,
                new CommandException(String.format("`%s` is not a valid achievement. %s",
                        achievementOpt, FormatUtil.options(Achievement.class))));

        return context.getClient()
                .getUserById(userId)
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> Mono.empty())
                .flatMap(user -> DatabaseManager.getUsers().getDBUser(user.getId())
                        .flatMap(dbUser -> switch (action) {
                            case ADD -> dbUser.unlockAchievement(achievement);
                            case REMOVE -> dbUser.lockAchievement(achievement);
                        })
                        .then(context.createFollowupMessage(Emoji.CHECK_MARK + " %s **%s** to **%s** done.",
                                FormatUtil.capitalizeEnum(action), achievement.getTitle(), user.getTag())));
    }

}
