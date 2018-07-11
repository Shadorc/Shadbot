package me.shadorc.shadbot.api.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResolveVanityUrlResponse {

	@JsonProperty("response")
	private SteamId response;

	public SteamId getResponse() {
		return response;
	}

	@Override
	public String toString() {
		return "ResolveVanityUrlResponse [response=" + response + "]";
	}
}
