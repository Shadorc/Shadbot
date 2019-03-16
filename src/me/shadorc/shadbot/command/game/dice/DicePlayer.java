package me.shadorc.shadbot.command.game.dice;

import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.game.Player;

public class DicePlayer extends Player {

	private final int number;

	public DicePlayer(Snowflake userId, int number) {
		super(userId);
		this.number = number;
	}

	public int getNumber() {
		return this.number;
	}

}
