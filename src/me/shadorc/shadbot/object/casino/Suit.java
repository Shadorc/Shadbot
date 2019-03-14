package me.shadorc.shadbot.object.casino;

import java.awt.Color;

import me.shadorc.shadbot.object.Emoji;

public enum Suit {
	HEART(Color.RED, Emoji.HEARTS),
	DIAMOND(Color.RED, Emoji.DIAMONDS),
	CLUB(Color.BLACK, Emoji.CLUBS),
	SPADE(Color.BLACK, Emoji.SPADES);

	private final Color color;
	private final Emoji emoji;

	private Suit(Color color, Emoji emoji) {
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
