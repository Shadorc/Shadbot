package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroEliminations {

    @JsonProperty("hero")
    private String hero;
    @JsonProperty("eliminations_per_life")
    private String eliminationsPerLife;

    public String getHero() {
        return this.hero;
    }

    public String getEliminationsPerLife() {
        return this.eliminationsPerLife;
    }

    @Override
    public String toString() {
        return "HeroEliminations{" +
                "hero='" + this.hero + '\'' +
                ", eliminationsPerLife='" + this.eliminationsPerLife + '\'' +
                '}';
    }
}
