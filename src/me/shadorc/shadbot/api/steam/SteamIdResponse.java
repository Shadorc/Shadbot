package me.shadorc.shadbot.api.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamIdResponse {

	@JsonProperty("steamid")
	private String steamId;

	public String getSteamId() {
		return steamId;
	}

}