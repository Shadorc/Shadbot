package me.shadorc.shadbot.api.steam.stats;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerStats {

	@Nullable
	@JsonProperty("stats")
	private List<Stats> stats;

	public List<Stats> getStats() {
		return stats;
	}

	@Override
	public String toString() {
		return String.format("PlayerStats [stats=%s]", stats);
	}

}
