package com.locibot.locibot.command.owner;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.CommandPermission;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.DiscordUtil;
import com.locibot.locibot.utils.FormatUtil;
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
        this.addOption("action", "Whether to add or remove an achievment", true, ApplicationCommandOptionType.STRING,
                DiscordUtil.toOptions(Action.class));
        this.addOption("achievement", "The achievement", true,
                ApplicationCommandOptionType.STRING, DiscordUtil.toOptions(Achievement.class));
        this.addOption("user", "The user", true, ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        final Action action = context.getOptionAsEnum(Action.class, "action").orElseThrow();
        final Achievement achievement = context.getOptionAsEnum(Achievement.class, "achievement").orElseThrow();

        return context.getOptionAsUser("user")
                .switchIfEmpty(Mono.error(new CommandException("User not found.")))
                .onErrorResume(ClientException.isStatusCode(HttpResponseStatus.FORBIDDEN.code()),
                        err -> Mono.empty())
                .flatMap(user -> DatabaseManager.getUsers().getDBUser(user.getId())
                        .flatMap(dbUser -> switch (action) {
                            case ADD -> dbUser.unlockAchievement(achievement);
                            case REMOVE -> dbUser.lockAchievement(achievement);
                        })
                        .then(context.createFollowupMessage(Emoji.CHECK_MARK, "%s achievement **%s** to **%s** done."
                                .formatted(FormatUtil.capitalizeEnum(action), achievement.getTitle(context),
                                        user.getTag()))));
    }

}
