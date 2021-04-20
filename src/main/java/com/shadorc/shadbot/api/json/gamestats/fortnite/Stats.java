package com.shadorc.shadbot.api.json.gamestats.fortnite;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Objects;

public record Stats(@Nullable @JsonProperty("p2") SubStats soloStats,
                    @Nullable @JsonProperty("p10") SubStats duoStats,
                    @Nullable @JsonProperty("p9") SubStats squadStats,
                    @Nullable @JsonProperty("curr_p2") SubStats seasonSoloStats,
                    @Nullable @JsonProperty("curr_p10") SubStats seasonDuoStats,
                    @Nullable @JsonProperty("curr_p9") SubStats seasonSquadStats) {

    public SubStats getSoloStats() {
        return Objects.requireNonNullElse(this.soloStats, SubStats.DEFAULT);
    }

    public SubStats getDuoStats() {
        return Objects.requireNonNullElse(this.duoStats, SubStats.DEFAULT);
    }

    public SubStats getSquadStats() {
        return Objects.requireNonNullElse(this.squadStats, SubStats.DEFAULT);
    }

    public SubStats getSeasonSoloStats() {
        return Objects.requireNonNullElse(this.seasonSoloStats, SubStats.DEFAULT);
    }

    public SubStats getSeasonDuoStats() {
        return Objects.requireNonNullElse(this.seasonDuoStats, SubStats.DEFAULT);
    }

    public SubStats getSeasonSquadStats() {
        return Objects.requireNonNullElse(this.seasonSquadStats, SubStats.DEFAULT);
    }

}
