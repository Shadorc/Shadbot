package me.shadorc.shadbot.api.steam.stats;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserStatsForGameResponse {

	@Nullable
	@JsonProperty("playerstats")
	private PlayerStats playerStats;

	public PlayerStats getPlayerStats() {
		return playerStats;
	}

	@Override
	public String toString() {
		return String.format("UserStatsForGameResponse [playerStats=%s]", playerStats);
	}

}
