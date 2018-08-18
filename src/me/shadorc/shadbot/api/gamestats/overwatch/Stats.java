package me.shadorc.shadbot.api.gamestats.overwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

	@JsonProperty("top_heroes")
	private TopHeroes topHeroes;
	@JsonProperty("game")
	private Game game;

	public TopHeroes getTopHeroes() {
		return topHeroes;
	}

	public Game getGame() {
		return game;
	}

	@Override
	public String toString() {
		return String.format("Stats [topHeroes=%s, game=%s]", topHeroes, game);
	}

}
