package com.shadorc.shadbot.api.json.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PlayerSummary(@JsonProperty("steamid") String steamId,
                            @JsonProperty("communityvisibilitystate") int communityVisibilityState,
                            @JsonProperty("avatarfull") String avatarFull,
                            @JsonProperty("personaname") String personaName) {

    public CommunityVisibilityState getCommunityVisibilityState() {
        return CommunityVisibilityState.valueOf(this.communityVisibilityState);
    }

    public enum CommunityVisibilityState {
        PRIVATE,
        FRIENDS_ONLY,
        PUBLIC;

        public static CommunityVisibilityState valueOf(int state) {
            return switch (state) {
                case 1 -> PRIVATE;
                case 2 -> FRIENDS_ONLY;
                case 3 -> PUBLIC;
                default -> throw new AssertionError();
            };
        }
    }

}