package com.shadorc.shadbot.api.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class Response {

    @Nullable
    @JsonProperty("steamid")
    private String steamId;

    public Optional<String> getSteamId() {
        return Optional.ofNullable(this.steamId);
    }

}