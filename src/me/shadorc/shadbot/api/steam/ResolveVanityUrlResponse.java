package me.shadorc.shadbot.api.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolveVanityUrlResponse {

	@JsonProperty("response")
	private SteamIdResponse response;

	public SteamIdResponse getResponse() {
		return response;
	}
}
