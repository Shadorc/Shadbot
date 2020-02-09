package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public class PlayerStats {

    @Nullable
    @JsonProperty("stats")
    private List<Stats> stats;

    public Optional<List<Stats>> getStats() {
        return Optional.ofNullable(this.stats);
    }

}
