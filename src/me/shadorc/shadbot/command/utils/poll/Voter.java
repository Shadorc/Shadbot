package me.shadorc.shadbot.command.utils.poll;

import discord4j.core.object.util.Snowflake;

class Voter {

	private final Snowflake userId;
	private String choice;

	protected Voter(Snowflake userId, String choice) {
		this.userId = userId;
		this.choice = choice;
	}

	protected Snowflake getId() {
		return userId;
	}

	protected String getChoice() {
		return choice;
	}

	protected void setChoice(String choice) {
		this.choice = choice;
	}

}
