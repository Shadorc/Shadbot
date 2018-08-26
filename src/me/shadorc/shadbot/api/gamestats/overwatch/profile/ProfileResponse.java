package me.shadorc.shadbot.api.gamestats.overwatch.profile;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProfileResponse {

	@JsonProperty("username")
	private String username;
	@JsonProperty("level")
	private int level;
	@JsonProperty("portrait")
	private String portrait;
	@JsonProperty("private")
	private boolean isPrivate;
	@JsonProperty("games")
	private Games games;
	@JsonProperty("playtime")
	private Map<String, String> playtime;
	@JsonProperty("competitive")
	private Map<String, String> competitive;

	public String getUsername() {
		return username;
	}

	public String getLevel() {
		return Integer.toString(level);
	}

	public String getPortrait() {
		return portrait;
	}

	public boolean isPrivate() {
		return isPrivate;
	}

	public Games getGames() {
		return games;
	}

	public String getRank() {
		return Objects.requireNonNullElse(competitive.get("rank"), "0");
	}

	public String getQuickplayPlaytime() {
		return playtime.get("quickplay");
	}

	@Override
	public String toString() {
		return String.format("Data [username=%s, level=%s, portrait=%s, isPrivate=%s, games=%s, playtime=%s, competitive=%s]", username, level, portrait, isPrivate, games, playtime, competitive);
	}

}
