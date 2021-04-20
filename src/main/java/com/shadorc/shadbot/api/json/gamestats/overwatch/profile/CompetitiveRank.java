package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import reactor.util.annotation.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

public record CompetitiveRank(@Nullable Integer rank) {

    public OptionalInt getRank() {
        return Optional.ofNullable(this.rank)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }

}
