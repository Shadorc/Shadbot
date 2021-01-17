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
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

public class BassBoostCmd extends BaseCmd {

    private static final int VALUE_MIN = 0;
    private static final int VALUE_MAX = 200;

    public BassBoostCmd() {
        super(CommandCategory.MUSIC, "bass_boost", "Drop the bass");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("percentage")
                        .description(String.format("must be between **%d%%** and **%d%%**.", VALUE_MIN, VALUE_MAX))
                        .type(ApplicationCommandOptionType.INTEGER.getValue())
                        .required(true)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        return DatabaseManager.getUsers().getDBUser(context.getAuthorId())
                .map(DBUser::getAchievements)
                .filter(achievements -> achievements.contains(Achievement.VOTER))
                .switchIfEmpty(Mono.error(new CommandException(
                        String.format("You can use this command by contributing to Shadbot <%s> or by unlocking the " +
                                        "**%s** achievement (more information using `/achievements`).",
                                Config.PATREON_URL, Achievement.VOTER.getTitle()))))
                .map(__ -> {
                    final String arg = context.getOption("percentage").orElseThrow();

                    final Integer percentage = NumberUtil.toIntBetweenOrNull(arg, VALUE_MIN, VALUE_MAX);
                    if (percentage == null) {
                        throw new CommandException(
                                String.format("Incorrect value. Must be between **%d** and **%d**.", VALUE_MIN, VALUE_MAX));
                    }

                    context.requireGuildMusic()
                            .getTrackScheduler()
                            .bassBoost(percentage);

                    if (percentage == 0) {
                        return String.format(Emoji.CHECK_MARK + " Bass boost disabled by **%s**.", context.getAuthorName());
                    }

                    return String.format(Emoji.CHECK_MARK + " Bass boost set to **%d%%** by **%s**.", percentage,
                            context.getAuthorName());
                })
                .flatMap(context::createFollowupMessage);
    }

}
