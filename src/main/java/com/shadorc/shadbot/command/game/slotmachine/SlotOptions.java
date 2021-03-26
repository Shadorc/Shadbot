package com.shadorc.shadbot.command.game.slotmachine;

import com.shadorc.shadbot.object.Emoji;

public enum SlotOptions {

    APPLE(Emoji.APPLE, 100),
    CHERRIES(Emoji.CHERRIES, 300),
    BELL(Emoji.BELL, 2500),
    GIFT(Emoji.GIFT, 30000);

    private final int gains;
    private final Emoji emoji;

    SlotOptions(Emoji emoji, int gains) {
        this.emoji = emoji;
        this.gains = gains;
    }

    public Emoji getEmoji() {
        return this.emoji;
    }

    public int getGains() {
        return this.gains;
    }
}
