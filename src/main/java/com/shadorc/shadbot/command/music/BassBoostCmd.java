package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.users.entity.DBUser;
import com.shadorc.shadbot.database.users.entity.achievement.Achievement;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class BassBoostCmd extends BaseCmd {

    private static final int BASSBOOST_MIN = 0;
    private static final int BASSBOOST_MAX = 200;

    public BassBoostCmd() {
        super(CommandCategory.MUSIC, "bass_boost", "Show or set current bass boost level");
        this.addOption(option -> option.name("percentage")
                .description("Bass boost to set, must be between %d%% and %d%%."
                        .formatted(BASSBOOST_MIN, BASSBOOST_MAX))
                .required(false)
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
                    final Optional<Long> percentageOpt = context.getOptionAsLong("percentage");
                    final TrackScheduler trackScheduler = context.requireGuildMusic().getTrackScheduler();
                    if (percentageOpt.isEmpty()) {
                        return context.createFollowupMessage(Emoji.LOUD_SOUND, context.localize("bassboost.current")
                                .formatted(trackScheduler.getBassBoostPercentage()));
                    }

                    final long percentage = percentageOpt.orElseThrow();
                    if (!NumberUtil.isBetween(percentage, BASSBOOST_MIN, BASSBOOST_MAX)) {
                        return Mono.error(new CommandException(context.localize("bassboost.invalid")
                                .formatted(BASSBOOST_MIN, BASSBOOST_MAX)));
                    }

                    trackScheduler.bassBoost((int) percentage);
                    trackScheduler.clearBuffer(); // Instantly apply change

                    if (percentage == 0) {
                        return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("bassboost.disabled"));
                    }

                    return context.createFollowupMessage(Emoji.CHECK_MARK, context.localize("bassboost.message")
                            .formatted(percentage));
                });
    }

}
