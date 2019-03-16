package me.shadorc.shadbot.command.game.trivia;

import discord4j.core.object.util.Snowflake;

public class TriviaPlayer {

	private final Snowflake userId;
	private boolean hasAnswered;

	public TriviaPlayer(Snowflake userId) {
		this.userId = userId;
		this.hasAnswered = false;
	}

	public Snowflake getUserId() {
		return this.userId;
	}

	public boolean hasAnswered() {
		return this.hasAnswered;
	}

	public void setAnswered(boolean hasAnswered) {
		this.hasAnswered = hasAnswered;
	}

}
