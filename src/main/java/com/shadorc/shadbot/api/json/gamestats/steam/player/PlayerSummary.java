package com.shadorc.shadbot.api.json.gamestats.steam.player;

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
            return switch (state) {
                case 1 -> PRIVATE;
                case 2 -> FRIENDS_ONLY;
                case 3 -> PUBLIC;
                default -> throw new AssertionError();
            };
        }
    }

    @Override
    public String toString() {
        return "PlayerSummary{" +
                "steamId='" + this.steamId + '\'' +
                ", communityVisibilityState=" + this.communityVisibilityState +
                ", avatarFull='" + this.avatarFull + '\'' +
                ", personaName='" + this.personaName + '\'' +
                '}';
    }
}