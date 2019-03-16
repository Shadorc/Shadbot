package me.shadorc.shadbot.command.game.roulette;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.Player;

public class RoulettePlayer extends Player {

	private final int bet;
	private final String place;

	public RoulettePlayer(Snowflake userId, int bet, String place) {
		super(userId);
		this.bet = bet;
		this.place = place;
	}

	public int getBet() {
		return bet;
	}

	public String getPlace() {
		return place;
	}

}
