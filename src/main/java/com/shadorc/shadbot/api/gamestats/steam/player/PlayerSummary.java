package com.shadorc.shadbot.api.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummary {

    @JsonProperty("steamid")
    private String steamId;
    @JsonProperty("communityvisibilitystate")
    private int communityVisibilityState;
    @JsonProperty("avatarfull")
    private String avatarFull;
    @JsonProperty("personaname")
    private String personaName;

    public String getSteamId() {
        return this.steamId;
    }

    public CommunityVisibilityState getCommunityVisibilityState() {
        return CommunityVisibilityState.valueOf(this.communityVisibilityState);
    }

    public String getAvatarFull() {
        return this.avatarFull;
    }

    public String getPersonaName() {
        return this.personaName;
    }

    public enum CommunityVisibilityState {
        PRIVATE,
        FRIENDS_ONLY,
        PUBLIC;

        public static CommunityVisibilityState valueOf(int state) {
            switch (state) {
                case 1:
                    return PRIVATE;
                case 2:
                    return FRIENDS_ONLY;
                case 3:
                    return PUBLIC;
                default:
                    throw new AssertionError();
            }
        }
    }

}