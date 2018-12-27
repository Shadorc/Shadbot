package me.shadorc.shadbot.api.gamestats.steam.stats;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerStats {

	@Nullable
	@JsonProperty("stats")
	private List<Stats> stats;

	public List<Stats> getStats() {
		return this.stats;
	}

}
