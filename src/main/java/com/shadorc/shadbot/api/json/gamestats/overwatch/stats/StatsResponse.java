package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatsResponse {

    @JsonProperty("stats")
    private Stats stats;

    public Stats getStats() {
        return this.stats;
    }

    @Override
    public String toString() {
        return "StatsResponse{" +
                "stats=" + this.stats +
                '}';
    }
}
