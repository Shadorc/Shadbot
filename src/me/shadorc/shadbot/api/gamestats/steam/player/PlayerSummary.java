package me.shadorc.shadbot.api.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummary {

	@JsonProperty("communityvisibilitystate")
	private int communityVisibilityState;
	@JsonProperty("avatarfull")
	private String avatarFull;
	@JsonProperty("personaname")
	private String personaName;

	/**
	 * @return 1: Private 2: FriendsOnly 3: Public
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

	@Override
	public String toString() {
		return String.format("PlayerSummary [communityVisibilityState=%s, avatarFull=%s, personaName=%s]", communityVisibilityState, avatarFull, personaName);
	}
}