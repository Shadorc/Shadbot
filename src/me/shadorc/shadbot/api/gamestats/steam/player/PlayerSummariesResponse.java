package me.shadorc.shadbot.api.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummariesResponse {

	@JsonProperty("response")
	private PlayerSummaries response;

	public PlayerSummaries getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return String.format("PlayerSummariesResponse [response=%s]", response);
	}

}
