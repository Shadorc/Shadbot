package me.shadorc.shadbot.api.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SteamId {

    @JsonProperty("steamid")
    private String steamId;

    public String getSteamId() {
        return this.steamId;
    }

}