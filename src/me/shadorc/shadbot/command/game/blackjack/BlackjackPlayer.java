package me.shadorc.shadbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.List;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.object.Card;

public class BlackjackPlayer {

	private final Snowflake userId;
	private final int bet;
	private final List<Card> cards;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(Snowflake userId, int bet) {
		this.userId = userId;
		this.bet = bet;
		this.cards = new ArrayList<>();
		this.isDoubleDown = false;
		this.isStanding = false;

		this.addCards(Card.pick(2));
	}

	public void hit() {
		this.addCards(List.of(Card.pick()));
	}

	public void stand() {
		this.isStanding = true;
	}

	public void doubleDown() {
		this.isDoubleDown = true;
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
		return this.userId;
	}

	public int getBet() {
		return this.bet * (this.isDoubleDown() ? 2 : 1);
	}

	public List<Card> getCards() {
		return this.cards;
	}

	public boolean isDoubleDown() {
		return this.isDoubleDown;
	}

	public boolean isStanding() {
		return this.isStanding;
	}

}