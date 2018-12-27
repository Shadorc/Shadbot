package me.shadorc.shadbot.api.gamestats.fortnite;

import java.util.Optional;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FortniteResponse {

	@Nullable
	@JsonProperty("error")
	private String error;
	@JsonProperty("stats")
	private Stats stats;

	public Optional<String> getError() {
		return Optional.ofNullable(this.error);
	}

	public Stats getStats() {
		return this.stats;
	}

}
