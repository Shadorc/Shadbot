package me.shadorc.shadbot.command.game.blackjack;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.object.casino.Card;
import me.shadorc.shadbot.object.casino.Hand;

public class BlackjackPlayer {

	private final Snowflake userId;
	private final String username;
	private final int bet;
	private final Hand hand;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(Snowflake userId, String username, int bet) {
		this.userId = userId;
		this.username = username;
		this.bet = bet;
		this.hand = new Hand();
		this.isDoubleDown = false;
		this.isStanding = false;
	}

	public void hit(Card card) {
		this.hand.deal(card);

		if(this.hand.getValue() >= 21) {
			this.stand();
		}
	}

	public void stand() {
		this.isStanding = true;
	}

	public void doubleDown(Card card) {
		this.isDoubleDown = true;
		this.hit(card);
		this.stand();
	}

	public EmbedFieldEntity format() {
		final StringBuilder name = new StringBuilder(String.format("%s's hand", this.getUsername()));
		if(this.isStanding()) {
			name.append(" (Stand)");
		}
		if(this.isDoubleDown()) {
			name.append(" (Double down)");
		}

		return new EmbedFieldEntity(name.toString(), this.hand.format(), true);
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

	public Hand getHand() {
		return this.hand;
	}

	public boolean isDoubleDown() {
		return this.isDoubleDown;
	}

	public boolean isStanding() {
		return this.isStanding;
	}

}