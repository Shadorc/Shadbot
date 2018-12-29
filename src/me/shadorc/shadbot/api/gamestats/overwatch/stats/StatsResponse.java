package me.shadorc.shadbot.api.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatsResponse {

	@JsonProperty("stats")
	private Stats stats;

	public Stats getStats() {
		return this.stats;
	}

}
