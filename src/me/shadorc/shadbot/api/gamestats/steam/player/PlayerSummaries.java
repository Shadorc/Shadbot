package me.shadorc.shadbot.api.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PlayerSummaries {

	@JsonProperty("players")
	private List<PlayerSummary> players;

	public List<PlayerSummary> getPlayers() {
		return this.players;
	}

}
