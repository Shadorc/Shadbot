package me.shadorc.shadbot.api.gamestats.steam.stats;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserStatsForGameResponse {

	@Nullable
	@JsonProperty("playerstats")
	private PlayerStats playerStats;

	public PlayerStats getPlayerStats() {
		return this.playerStats;
	}

	@Override
	public String toString() {
		return String.format("UserStatsForGameResponse [playerStats=%s]", this.playerStats);
	}

}
