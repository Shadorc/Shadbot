package me.shadorc.shadbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.List;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.object.Card;

public class BlackjackPlayer {

	private final Snowflake userId;
	private final String username;
	private final int bet;
	private final List<Card> cards;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(Snowflake userId, String username, int bet) {
		this.userId = userId;
		this.username = username;
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

	public EmbedFieldEntity formatHand() {
		final StringBuilder name = new StringBuilder(String.format("%s's hand", this.getUsername()));
		if(this.isStanding()) {
			name.append(" (Stand)");
		}
		if(this.isDoubleDown()) {
			name.append(" (Double down)");
		}

		return new EmbedFieldEntity(name.toString(), BlackjackUtils.formatCards(this.getCards()), true);
	}

	public Snowflake getUserId() {
		return this.userId;
	}

	public String getUsername() {
		return this.username;
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