package me.shadorc.shadbot.api.steam;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayersResponse {

	@JsonProperty("players")
	private List<PlayerResponse> players;

	public List<PlayerResponse> getPlayers() {
		return players;
	}

}