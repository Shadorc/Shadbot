package me.shadorc.shadbot.api.gamestats.overwatch.profile;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Games {

	@JsonProperty("quickplay")
	private Map<String, Integer> quickplay;

	public String getQuickplayWon() {
		return this.quickplay.getOrDefault("won", 0).toString();
	}

}
