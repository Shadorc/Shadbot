package com.shadorc.shadbot.command.music;

import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.NumberUtil;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class BassBoostCmd extends BaseCmd {

    private static final int VALUE_MIN = 0;
    private static final int VALUE_MAX = 200;

    public BassBoostCmd() {
        super(CommandCategory.MUSIC, "bass_boost", "Drop the bass");
        this.addOption("percentage", "Bass boost in percent, must be between **%d%%** and **%d%%**."
                .formatted(VALUE_MIN, VALUE_MAX), true, ApplicationCommandOptionType.INTEGER);
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId())
                .map(DBUser::getAchievements)
                .filter(achievements -> achievements.contains(Achievement.VOTER))
                .switchIfEmpty(Mono.error(new CommandException(context.localize("bassboost.unlock")
                        .formatted(Config.PATREON_URL, Achievement.VOTER.getTitle()))))
                .flatMap(__ -> {
                    final String arg = context.getOptionAsString("percentage").orElseThrow();

                    final Integer percentage = NumberUtil.toIntBetweenOrNull(arg, VALUE_MIN, VALUE_MAX);
                    if (percentage == null) {
                        return Mono.error(new CommandException(context.localize("bassboost.invalid")
                                .formatted(VALUE_MIN, VALUE_MAX)));
                    }

                    context.requireGuildMusic()
                            .getTrackScheduler()
                            .bassBoost(percentage);

                    if (percentage == 0) {
                        return context.reply(Emoji.CHECK_MARK, context.localize("bassboost.disabled"));
                    }

                    return context.reply(Emoji.CHECK_MARK, context.localize("bassboost.message").formatted(percentage));
                });
    }

}
