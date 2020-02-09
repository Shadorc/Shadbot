package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Competitive {

    @JsonProperty("tank")
    private CompetitiveRank tank;
    @JsonProperty("damage")
    private CompetitiveRank damage;
    @JsonProperty("support")
    private CompetitiveRank support;

    public CompetitiveRank getTank() {
        return this.tank;
    }

    public CompetitiveRank getDamage() {
        return this.damage;
    }

    public CompetitiveRank getSupport() {
        return this.support;
    }
}
