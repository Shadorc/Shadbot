package me.shadorc.shadbot.api.gamestats.steam.player;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummaries {

	@JsonProperty("players")
	private List<PlayerSummary> players;

	public List<PlayerSummary> getPlayers() {
		return this.players;
	}

}
