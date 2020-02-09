package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

    @JsonProperty("top_heroes")
    private TopHeroes topHeroes;

    public TopHeroes getTopHeroes() {
        return this.topHeroes;
    }

}
