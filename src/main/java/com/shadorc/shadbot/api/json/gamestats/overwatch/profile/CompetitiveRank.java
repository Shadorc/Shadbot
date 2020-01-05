package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Optional;
import java.util.OptionalInt;

public class CompetitiveRank {

    @JsonProperty("rank")
    @Nullable
    private Integer rank;

    public OptionalInt getRank() {
        return Optional.ofNullable(this.rank)
                .map(OptionalInt::of)
                .orElse(OptionalInt.empty());
    }
}
