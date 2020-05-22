package com.shadorc.shadbot.command.info;

import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.CommandHelpBuilder;
import com.shadorc.shadbot.utils.DiscordUtils;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Consumer;

public class AchievementsCmd extends BaseCmd {

    public AchievementsCmd() {
        super(CommandCategory.INFO, List.of("achievements", "achievement"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        return context.getGuild()
                .flatMap(guild -> DiscordUtils.extractMemberOrAuthor(guild, context.getMessage()))
                .flatMap(member -> DatabaseManager.getUsers().getDBUser(member.getId())
                        .map(DBUser::getAchievements)
                        .map(achievements -> DiscordUtils.getDefaultEmbed()
                                .andThen(embed -> {
                                    embed.setAuthor(String.format("%s's Achievements", member.getUsername()),
                                            null, member.getAvatarUrl());
                                    embed.setThumbnail("https://i.imgur.com/IMHDI7D.png");
                                    embed.setTitle(String.format("%d/%d achievements unlocked.",
                                            achievements.size(), Achievement.values().length));

                                    final StringBuilder description = new StringBuilder();
                                    for (final Achievement achievement : achievements) {
                                        description.append(this.formatAchievement(achievement, true));
                                    }
                                    for (final Achievement achievement : Achievement.values()) {
                                        if (!achievements.contains(achievement)) {
                                            description.append(this.formatAchievement(achievement, false));
                                        }
                                    }
                                    embed.setDescription(description.toString());
                                })))
                .flatMap(embed -> context.getChannel()
                        .flatMap(channel -> DiscordUtils.sendMessage(embed, channel)))
                .then();
    }

    private String formatAchievement(Achievement achievement, boolean unlocked) {
        return String.format("%s **%s**%n%s%n",
                unlocked ? achievement.getEmoji() : Emoji.LOCK, achievement.getTitle(), achievement.getDescription());
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return CommandHelpBuilder.create(this, context)
                .setDescription("Show user's achievements.")
                .addArg("@user", "if not specified, it will show your achievements", true)
                .build();
    }

}
