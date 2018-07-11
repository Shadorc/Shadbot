package me.shadorc.shadbot.api.steam.player;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummaries {

	@JsonProperty("players")
	private List<PlayerSummary> players;

	public List<PlayerSummary> getPlayers() {
		return players;
	}

	@Override
	public String toString() {
		return "PlayerSummaries [players=" + players + "]";
	}

}
