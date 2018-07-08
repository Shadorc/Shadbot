package me.shadorc.shadbot.api.steam;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserStatsForGameResponse {

	@Nullable
	@JsonProperty("playerstats")
	private PlayerStats playerStats;

	public PlayerStats getPlayerStats() {
		return playerStats;
	}

}
