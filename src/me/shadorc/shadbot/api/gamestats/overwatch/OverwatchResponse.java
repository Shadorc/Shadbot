package me.shadorc.shadbot.api.gamestats.overwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OverwatchResponse {

	@JsonProperty("username")
	private String username;
	@JsonProperty("level")
	private int level;
	@JsonProperty("portrait")
	private String portrait;
	@JsonProperty("private")
	private boolean isPrivate;
	@JsonProperty("stats")
	private Stats stats;

	public String getUsername() {
		return username;
	}

	public int getLevel() {
		return level;
	}

	public String getPortrait() {
		return portrait;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public Stats getStats() {
		return stats;
	}

	@Override
	public String toString() {
		return String.format("OverwatchResponse [username=%s, level=%s, portrait=%s, isPrivate=%s, stats=%s]", username, level, portrait, isPrivate, stats);
	}

}
