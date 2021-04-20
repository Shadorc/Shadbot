package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;

public record UserStatsForGameResponse(@Nullable @JsonProperty("playerstats") PlayerStats playerStats) {

    public Optional<PlayerStats> getPlayerStats() {
        return Optional.ofNullable(this.playerStats);
    }

}
