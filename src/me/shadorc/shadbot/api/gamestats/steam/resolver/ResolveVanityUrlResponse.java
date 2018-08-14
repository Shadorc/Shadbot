package me.shadorc.shadbot.api.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResolveVanityUrlResponse {

	@JsonProperty("response")
	private SteamId response;

	public SteamId getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return String.format("ResolveVanityUrlResponse [response=%s]", response);
	}
}
