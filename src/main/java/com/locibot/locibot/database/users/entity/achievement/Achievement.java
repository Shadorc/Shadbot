package com.locibot.locibot.database.users.entity.achievement;

import com.locibot.locibot.core.i18n.I18nContext;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.object.Emoji;

import java.util.EnumSet;

/**
 * Represents the various type of achievements.
 */
public enum Achievement {

    CROESUS(0, Emoji.GEM, "croesus.title", "croesus.description"),
    ENGINEER(1, Emoji.GEAR, "engineer.title", "engineer.description"),
    SUPERHERO(2, Emoji.HEARTS, "superhero.title", "superhero.description"),
    VOTER(3, Emoji.BALLOT_BOX, "voter.title", "voter.description"),
    BUG_FINDER(4, Emoji.BUG, "bug.finder.title", "bug.finder.description"),
    BINGO(5, Emoji.TICKET, "bingo.title", "bingo.description"),
    IMPROVER(6, Emoji.ROCKET, "improver.title", "improver.description"),
    MILLIONAIRE(7, Emoji.BANK, "millionaire.title", "millionaire.description");

    private final int value;
    private final Emoji emoji;
    private final String title;
    private final String description;

    Achievement(int value, Emoji emoji, String title, String description) {
        this.value = value;
        this.emoji = emoji;
        this.title = title;
        this.description = description;
    }

    public int getFlag() {
        return 1 << this.value;
    }

    public Emoji getEmoji() {
        return this.emoji;
    }

    public String getTitle(I18nContext context) {
        return context.localize(this.title);
    }

    public String getDescription(I18nContext context) {
        return context.localize(this.description)
                .replace("{patreon_url}", Config.PATREON_URL)
                .replace("{top_gg_url}", Config.TOP_GG_URL);
    }

    public static EnumSet<Achievement> of(int value) {
        final EnumSet<Achievement> achievements = EnumSet.noneOf(Achievement.class);
        for (final Achievement achievement : Achievement.values()) {
            final long achievementValue = achievement.getFlag();
            if ((achievementValue & value) == achievementValue) {
                achievements.add(achievement);
            }
        }
        return achievements;
    }
}
