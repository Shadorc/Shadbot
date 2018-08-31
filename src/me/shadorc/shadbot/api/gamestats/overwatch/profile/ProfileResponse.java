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
		return this.username;
	}

	public String getLevel() {
		return Integer.toString(this.level);
	}

	public String getPortrait() {
		return this.portrait;
	}

	public boolean isPrivate() {
		return this.isPrivate;
	}

	public Games getGames() {
		return this.games;
	}

	public String getRank() {
		return Objects.requireNonNullElse(this.competitive.get("rank"), "0");
	}

	public String getQuickplayPlaytime() {
		return this.playtime.get("quickplay");
	}

	@Override
	public String toString() {
		return String.format("Data [username=%s, level=%s, portrait=%s, isPrivate=%s, games=%s, playtime=%s, competitive=%s]", this.username, this.level, this.portrait, this.isPrivate, this.games, this.playtime, this.competitive);
	}

}
