package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public record PlayerStats(@Nullable List<Stats> stats) {

    public Optional<List<Stats>> getStats() {
        return Optional.ofNullable(this.stats);
    }

}
