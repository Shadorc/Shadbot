package com.locibot.locibot.command.music;

import com.locibot.locibot.command.CommandException;
import com.locibot.locibot.core.command.BaseCmd;
import com.locibot.locibot.core.command.CommandCategory;
import com.locibot.locibot.core.command.Context;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.users.entity.DBUser;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.locibot.locibot.object.Emoji;
import com.locibot.locibot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class BassBoostCmd extends BaseCmd {

    private static final int BASSBOOST_MIN = 0;
    private static final int BASSBOOST_MAX = 200;
    private int percentage;

    public BassBoostCmd() {
        super(CommandCategory.MUSIC, "bass_boost", "Drop the bass");
        this.addOption(option -> option.name("percentage")
                .description("Bass boost in percent, must be between %d%% and %d%%."
                        .formatted(BASSBOOST_MIN, BASSBOOST_MAX))
                .required(true)
                .type(ApplicationCommandOptionType.INTEGER.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId())
                .map(DBUser::getAchievements)
                .filter(achievements -> achievements.contains(Achievement.VOTER))
                .switchIfEmpty(Mono.error(new CommandException(context.localize("bassboost.unlock")
                        .formatted(Config.PATREON_URL, Achievement.VOTER.getTitle(context)))))
                .flatMap(__ -> {
                    final long percentage = context.getOptionAsLong("percentage").orElseThrow();
                    if (!NumberUtil.isBetween(percentage, BASSBOOST_MIN, BASSBOOST_MAX)) {
                        return Mono.error(new CommandException(context.localize("bassboost.invalid")
                                .formatted(BASSBOOST_MIN, BASSBOOST_MAX)));
                    }

                    context.requireGuildMusic()
                            .getTrackScheduler()
                            .bassBoost((int) percentage);

                    if (percentage == 0) {
                        return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("bassboost.disabled"));
                    }

                    return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("bassboost.message")
                            .formatted(percentage));
                });
    }

}
