package me.shadorc.shadbot.api.steam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerResponse {

	@JsonProperty("communityvisibilitystate")
	private int communityVisibilityState;
	@JsonProperty("avatarfull")
	private String avatarFull;
	@JsonProperty("personaname")
	private String personaName;

	/**
	 * 1: Private 2: FriendsOnly 3: Public
	 */
	public int getCommunityVisibilityState() {
		return communityVisibilityState;
	}

	public String getAvatarFull() {
		return avatarFull;
	}

	public String getPersonaName() {
		return personaName;
	}

}
