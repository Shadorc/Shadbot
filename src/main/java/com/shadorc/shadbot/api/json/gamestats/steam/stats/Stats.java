package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

    @JsonProperty("name")
    private String name;
    @JsonProperty("value")
    private int value;

    public String getName() {
        return this.name;
    }

    public int getValue() {
        return this.value;
    }

}
