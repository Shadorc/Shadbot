package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public class UserStatsForGameResponse {

    @Nullable
    @JsonProperty("playerstats")
    private PlayerStats playerStats;

    public Optional<PlayerStats> getPlayerStats() {
        return Optional.ofNullable(this.playerStats);
    }

}
