package me.shadorc.shadbot.command.game.roulette;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

public class RoulettePlayer {

	private final Snowflake userId;
	private final int bet;
	private final String place;

	public RoulettePlayer(Snowflake userId, int bet, String place) {
		this.userId = userId;
		this.bet = bet;
		this.place = place;
	}

	public Snowflake getUserId() {
		return userId;
	}

	public int getBet() {
		return bet;
	}

	public String getPlace() {
		return place;
	}

	public Mono<String> getUsername(DiscordClient client) {
		return client.getUserById(this.userId)
				.map(User::getUsername);
	}

}
