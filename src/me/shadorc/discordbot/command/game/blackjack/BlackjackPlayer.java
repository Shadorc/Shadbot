package me.shadorc.discordbot.command.game.blackjack;

import java.util.ArrayList;
import java.util.List;

import me.shadorc.discordbot.utils.game.Card;
import sx.blah.discord.handle.obj.IUser;

public class BlackjackPlayer {

	private final IUser user;
	private final int bet;
	private final List<Card> cards;

	private boolean doubleDown;
	private boolean isStanding;

	public BlackjackPlayer(IUser user, int bet) {
		this.user = user;
		this.bet = bet;
		this.cards = new ArrayList<>();
		this.doubleDown = false;
		this.isStanding = false;
	}

	public void hit() {
		this.addCards(BlackjackUtils.pickCards(1));
	}

	public void stand() {
		isStanding = true;
	}

	public void doubleDown() {
		doubleDown = true;
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
		return bet * (doubleDown ? 2 : 1);
	}

	public List<Card> getCards() {
		return cards;
	}

	public boolean hasDoubleDown() {
		return doubleDown;
	}

	public boolean isStanding() {
		return isStanding;
	}
}