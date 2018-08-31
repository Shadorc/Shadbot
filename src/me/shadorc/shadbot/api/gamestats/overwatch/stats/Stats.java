package me.shadorc.shadbot.api.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

	@JsonProperty("top_heroes")
	private TopHeroes topHeroes;

	public TopHeroes getTopHeroes() {
		return this.topHeroes;
	}

	@Override
	public String toString() {
		return String.format("Stats [topHeroes=%s]", this.topHeroes);
	}

}
