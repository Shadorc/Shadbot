package me.shadorc.shadbot.api.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopHeroes {

	@JsonProperty("quickplay")
	private Quickplay quickplay;

	public Quickplay getQuickplay() {
		return quickplay;
	}

	@Override
	public String toString() {
		return String.format("TopHeroes [quickplay=%s]", quickplay);
	}

}
