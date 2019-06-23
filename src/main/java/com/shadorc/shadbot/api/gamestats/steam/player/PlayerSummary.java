package com.shadorc.shadbot.api.gamestats.steam.player;

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
        return this.communityVisibilityState;
    }

    public String getAvatarFull() {
        return this.avatarFull;
    }

    public String getPersonaName() {
        return this.personaName;
    }

}