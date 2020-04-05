package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Games {

    @JsonProperty("quickplay")
    private Map<String, Integer> quickplay;

    public Integer getQuickplayWon() {
        return this.quickplay.get("won");
    }

    @Override
    public String toString() {
        return "Games{" +
                "quickplay=" + this.quickplay +
                '}';
    }
}
