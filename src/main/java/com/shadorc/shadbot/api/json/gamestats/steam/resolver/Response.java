package com.shadorc.shadbot.api.json.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public record Response(@Nullable @JsonProperty("steamid") String steamId) {

    public Optional<String> getSteamId() {
        return Optional.ofNullable(this.steamId);
    }

}