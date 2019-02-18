package me.shadorc.shadbot.api.gamestats.steam.stats;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import reactor.util.annotation.Nullable;

public class PlayerStats {

	@Nullable
	@JsonProperty("stats")
	private List<Stats> stats;

	public List<Stats> getStats() {
		return this.stats;
	}

}
