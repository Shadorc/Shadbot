package com.shadorc.shadbot.db.guilds.entity.achievement;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;

import java.util.EnumSet;

/**
 * Represents the various type of achievements.
 */
public enum Achievement {

    MONEY(0, Emoji.GEM, "Money money money", "Win 1 billion coins"),
    ENGINEER(1, Emoji.GEAR, "Engineer", "Change Shadbot's default settings"),
    SUPERHERO(2, Emoji.HEARTS, "Superhero", String.format("Contribute to [Shadbot](%s)", Config.PATREON_URL)),
    VOTER(3, Emoji.BALLOT_BOX, "Voter", String.format("Vote for Shadbot on [top.gg](%s)", Config.TOP_GG_URL)),
    BUG_FINDER(4, Emoji.BUG, "Bug finder", "Report a bug"),
    BINGO(5, Emoji.TICKET, "Bingo", "Win the lottery");

    private final int value;
    private final int flag;
    private final Emoji emoji;
    private final String title;
    private final String description;

    Achievement(int value, Emoji emoji, String title, String description) {
        this.value = value;
        this.flag = 1 << value;
        this.emoji = emoji;
        this.title = title;
        this.description = description;
    }

    public int getValue() {
        return this.value;
    }

    public int getFlag() {
        return this.flag;
    }

    public Emoji getEmoji() {
        return this.emoji;
    }

    public String getTitle() {
        return this.title;
    }

    public String getDescription() {
        return this.description;
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

    @Override
    public String toString() {
        return "Achievement{" +
                "value=" + this.value +
                ", flag=" + this.flag +
                ", emoji=" + this.emoji +
                ", title='" + this.title + '\'' +
                ", description='" + this.description + '\'' +
                '}';
    }
}
