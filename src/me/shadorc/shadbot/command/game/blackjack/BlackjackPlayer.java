package me.shadorc.shadbot.command.game.blackjack;

import java.util.List;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.command.Card;

public class BlackjackPlayer {

	private final Snowflake userId;
	private final int bet;
	private final List<Card> cards;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(Snowflake userId, int bet) {
		this.userId = userId;
		this.bet = bet;
		this.cards = Card.pick(2);
		this.isDoubleDown = false;
		this.isStanding = false;
	}

	public void hit() {
		this.addCards(List.of(Card.pick()));
	}

	public void stand() {
		isStanding = true;
	}

	public void doubleDown() {
		isDoubleDown = true;
		this.hit();
		this.stand();
	}

	public void addCards(List<Card> cards) {
		if(this.isStanding()) {
			return;
		}

		this.cards.addAll(cards);

		if(BlackjackUtils.getValue(this.cards) >= 21) {
			this.stand();
		}
	}

	public Snowflake getUserId() {
		return userId;
	}

	public int getBet() {
		return bet * (this.isDoubleDown() ? 2 : 1);
	}

	public List<Card> getCards() {
		return cards;
	}

	public boolean isDoubleDown() {
		return isDoubleDown;
	}

	public boolean isStanding() {
		return isStanding;
	}

}