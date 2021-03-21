package com.shadorc.shadbot.command.todo;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.core.i18n.I18nContext;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.core.object.entity.Member;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.function.Consumer;

class AchievementsCmd extends BaseCmd {

    public AchievementsCmd() {
        super(CommandCategory.INFO, "achievements", "Show user's achievements");
        this.addOption("user", "If not specified, it will show your achievements", false,
                ApplicationCommandOptionType.USER);
    }

    @Override
    public Mono<?> execute(Context context) {
        return context.getOptionAsMember("user")
                .defaultIfEmpty(context.getAuthor())
                .flatMap(member -> DatabaseManager.getUsers().getDBUser(member.getId())
                        .map(DBUser::getAchievements)
                        .map(achievements -> AchievementsCmd.formatEmbed(context, achievements, member)))
                .flatMap(context::reply);
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(I18nContext context, EnumSet<Achievement> achievements,
                                                         Member member) {
        return ShadbotUtil.getDefaultEmbed(embed -> {
            embed.setAuthor(context.localize("achievement.title").formatted(member.getUsername()),
                    null, member.getAvatarUrl());
            embed.setThumbnail("https://i.imgur.com/IMHDI7D.png");
            embed.setTitle(context.localize("achievement.progression")
                    .formatted(achievements.size(), Achievement.values().length));

            final StringBuilder description = new StringBuilder();
            for (final Achievement achievement : Achievement.values()) {
                description.append(
                        AchievementsCmd.formatAchievement(context, achievement, achievements.contains(achievement)));
            }
            embed.setDescription(description.toString());
        });
    }

    private static String formatAchievement(I18nContext context, Achievement achievement, boolean unlocked) {
        final Emoji emoji = unlocked ? achievement.getEmoji() : Emoji.LOCK;
        return "%s **%s**%n%s%n".formatted(emoji, achievement.getTitle(context), achievement.getDescription(context));
    }

}
