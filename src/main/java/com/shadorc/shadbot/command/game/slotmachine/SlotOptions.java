package com.shadorc.shadbot.command.game.slotmachine;

public enum SlotOptions {

    APPLE(100),
    CHERRIES(300),
    BELL(2500),
    GIFT(30000);

    private final int gains;

    SlotOptions(int gains) {
        this.gains = gains;
    }

    public int getGains() {
        return this.gains;
    }

    public String getEmoji() {
        return String.format(":%s:", this.toString().toLowerCase());
    }
}
