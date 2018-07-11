package me.shadorc.shadbot.api.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SteamId {

	@JsonProperty("steamid")
	private String steamId;

	public String getSteamId() {
		return steamId;
	}

	@Override
	public String toString() {
		return "SteamId [steamId=" + steamId + "]";
	}

}