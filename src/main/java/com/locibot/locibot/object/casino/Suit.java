package com.locibot.locibot.object.casino;

import com.locibot.locibot.object.Emoji;

import java.awt.*;

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

    public Emoji getEmoji() {
        return this.emoji;
    }
}
