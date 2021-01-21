package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.function.Consumer;

public class AchievementsCmd extends BaseCmd {

    public AchievementsCmd() {
        super(CommandCategory.INFO, "achievements", "Show user's achievements");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder.addOption(ApplicationCommandOptionData.builder()
                .name("user")
                .description("If not specified, it will show your achievements")
                .type(ApplicationCommandOptionType.USER.getValue())
                .required(false)
                .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(member -> DatabaseManager.getUsers().getDBUser(member.getId())
                        .map(DBUser::getAchievements)
                        .map(achievements -> AchievementsCmd.formatAchievements(achievements, member)))
                .flatMap(context::createFollowupMessage);
    }

    private static Consumer<EmbedCreateSpec> formatAchievements(EnumSet<Achievement> achievements, Member member) {
        return ShadbotUtil.getDefaultEmbed(embed -> {
            embed.setAuthor(String.format("%s's Achievements", member.getUsername()),
                    null, member.getAvatarUrl());
            embed.setThumbnail("https://i.imgur.com/IMHDI7D.png");
            embed.setTitle(String.format("%d/%d achievement(s) unlocked.",
                    achievements.size(), Achievement.values().length));

            final StringBuilder description = new StringBuilder();
            for (final Achievement achievement : Achievement.values()) {
                description.append(AchievementsCmd.formatAchievement(achievement, achievements.contains(achievement)));
            }
            embed.setDescription(description.toString());
        });
    }

    private static String formatAchievement(Achievement achievement, boolean unlocked) {
        return String.format("%s **%s**%n%s%n",
                unlocked ? achievement.getEmoji() : Emoji.LOCK, achievement.getTitle(), achievement.getDescription());
    }

}
