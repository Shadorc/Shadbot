package me.shadorc.shadbot.api.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatsResponse {

	@JsonProperty("stats")
	private Stats stats;

	public Stats getStats() {
		return stats;
	}

	@Override
	public String toString() {
		return String.format("StatsResponse [stats=%s]", stats);
	}

}
