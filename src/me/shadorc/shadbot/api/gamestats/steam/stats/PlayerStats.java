package me.shadorc.shadbot.api.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;

public class PlayerStats {

	@Nullable
	@JsonProperty("stats")
	private List<Stats> stats;

	public List<Stats> getStats() {
		return this.stats;
	}

}
