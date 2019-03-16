package me.shadorc.shadbot.command.game.dice;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class DicePlayer {

	private final Snowflake userId;
	private final int number;

	public DicePlayer(Snowflake userId, int number) {
		this.userId = userId;
		this.number = number;
	}

	public Snowflake getUserId() {
		return this.userId;
	}

	public int getNumber() {
		return this.number;
	}

	public Mono<String> getUsername(DiscordClient client) {
		return client.getUserById(this.getUserId())
				.map(User::getUsername);
	}

}
