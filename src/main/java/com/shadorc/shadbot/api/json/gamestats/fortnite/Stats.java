package com.shadorc.shadbot.api.json.gamestats.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record Stats(@JsonProperty("p2") Optional<SubStats> soloStats,
                    @JsonProperty("p10") Optional<SubStats> duoStats,
                    @JsonProperty("p9") Optional<SubStats> squadStats,
                    @JsonProperty("curr_p2") Optional<SubStats> seasonSoloStats,
                    @JsonProperty("curr_p10") Optional<SubStats> seasonDuoStats,
                    @JsonProperty("curr_p9") Optional<SubStats> seasonSquadStats) {

    public SubStats getSoloStats() {
        return this.soloStats.orElse(SubStats.DEFAULT);
    }

    public SubStats getDuoStats() {
        return this.duoStats.orElse(SubStats.DEFAULT);
    }

    public SubStats getSquadStats() {
        return this.squadStats.orElse(SubStats.DEFAULT);
    }

    public SubStats getSeasonSoloStats() {
        return this.seasonSoloStats.orElse(SubStats.DEFAULT);
    }

    public SubStats getSeasonDuoStats() {
        return this.seasonDuoStats.orElse(SubStats.DEFAULT);
    }

    public SubStats getSeasonSquadStats() {
        return this.seasonSquadStats.orElse(SubStats.DEFAULT);
    }

}
