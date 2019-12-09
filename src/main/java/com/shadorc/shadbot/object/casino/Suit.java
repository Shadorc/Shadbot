package com.shadorc.shadbot.object.casino;

import com.shadorc.shadbot.object.Emoji;

import java.awt.Color;

public enum Suit {
    HEART(Color.RED, Emoji.HEARTS),
    DIAMOND(Color.RED, Emoji.DIAMONDS),
    CLUB(Color.BLACK, Emoji.CLUBS),
    SPADE(Color.BLACK, Emoji.SPADES);

    private final Color color;
    private final Emoji emoji;

    Suit(Color color, Emoji emoji) {
        this.color = color;
        this.emoji = emoji;
    }

    public boolean isRed() {
        return this.color.equals(Color.RED);
    }

    public boolean isBlack() {
        return !this.isRed();
    }

    public Emoji getEmoji() {
        return this.emoji;
    }
}
