package me.shadorc.shadbot.object.casino;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.shadorc.shadbot.utils.FormatUtils;

public class Hand {

	private final List<Card> cards;

	public Hand(List<Card> cards) {
		this.cards = new ArrayList<>();
		this.deal(cards);
	}

	public Hand() {
		this(Collections.emptyList());
	}

	public void deal(Card card) {
		this.cards.add(card);
	}

	public void deal(List<Card> cards) {
		this.cards.addAll(cards);
	}

	public int count() {
		return this.getCards().size();
	}

	public List<Card> getCards() {
		return Collections.unmodifiableList(this.cards);
	}

	public int getValue() {
		int aceCount = 0;

		int value = 0;
		for(final Card card : cards) {
			if(card.getValue().equals(Value.ACE)) {
				aceCount++;
			} else {
				// King, Queen and Jack have a number superior to 10 but their value is 10
				value += Math.min(10, card.getValue().getNumeric());
			}
		}

		if(aceCount > 0) {
			value += aceCount - 1;
			value += value + 11 > 21 ? 1 : 11;
		}

		return value;
	}

	public String format() {
		return String.format("%s%nValue: %d",
				FormatUtils.format(this.getCards(), card -> String.format("`%s` %s", card.getValue().getIdent(), card.getSuit().getEmoji()), " | "),
				this.getValue());
	}

}
