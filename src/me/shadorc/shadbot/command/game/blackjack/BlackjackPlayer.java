package me.shadorc.shadbot.command.game.blackjack;

import java.util.List;

import me.shadorc.shadbot.utils.object.Card;

public class BlackjackPlayer {

	private final IUser user;
	private final int bet;
	private final List<Card> cards;

	private boolean isDoubleDown;
	private boolean isStanding;

	public BlackjackPlayer(IUser user, int bet) {
		this.user = user;
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

	public IUser getUser() {
		return user;
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