package me.shadorc.shadbot.command.game.blackjack;

import discord4j.common.json.EmbedFieldEntity;
import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.Player;
import me.shadorc.shadbot.object.casino.Card;
import me.shadorc.shadbot.object.casino.Hand;
import reactor.core.publisher.Mono;

public class BlackjackPlayer extends Player {

	private final int bet;
	private final Hand hand;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(Snowflake userId, int bet) {
		super(userId);
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

	public Mono<EmbedFieldEntity> format(DiscordClient client) {
		return this.getUsername(client)
				.map(username -> {
					final StringBuilder name = new StringBuilder(String.format("%s's hand", username));
					if(this.isStanding()) {
						name.append(" (Stand)");
					}
					if(this.isDoubleDown()) {
						name.append(" (Double down)");
					}

					return new EmbedFieldEntity(name.toString(), this.hand.format(), true);
				});
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