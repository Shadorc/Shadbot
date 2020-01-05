package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopHeroes {

    @JsonProperty("quickplay")
    private Quickplay quickplay;

    public Quickplay getQuickplay() {
        return this.quickplay;
    }

}
