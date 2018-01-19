package me.shadorc.shadbot.utils.object;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Card {

	private final int num;
	private final String name;
	private final Sign sign;

	public enum Sign {
		HEART(Color.RED, Emoji.HEARTS),
		TILE(Color.RED, Emoji.DIAMONDS),
		CLOVER(Color.BLACK, Emoji.CLUBS),
		PIKE(Color.BLACK, Emoji.SPADES);

		private final Color color;
		private final Emoji emoji;

		Sign(Color color, Emoji emoji) {
			this.color = color;
			this.emoji = emoji;
		}

		public Color getColor() {
			return color;
		}

		public Emoji getEmoji() {
			return emoji;
		}
	}

	public Card(int num, Sign sign) {
		this.num = num;
		switch (num) {
			case 11:
				this.name = "J";
				break;
			case 12:
				this.name = "Q";
				break;
			case 13:
				this.name = "K";
				break;
			default:
				this.name = Integer.toString(num);
				break;
		}
		this.sign = sign;
	}

	public int getNum() {
		return num;
	}

	public String getName() {
		return name;
	}

	public Sign getSign() {
		return sign;
	}

	public static Card pick() {
		return new Card(ThreadLocalRandom.current().nextInt(1, 14), Sign.values()[ThreadLocalRandom.current().nextInt(Sign.values().length)]);
	}

	public static List<Card> pick(int count) {
		List<Card> cards = new ArrayList<>();
		for(int i = 0; i < count; i++) {
			cards.add(Card.pick());
		}
		return cards;
	}
}
