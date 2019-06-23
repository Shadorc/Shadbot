package com.shadorc.shadbot.api.gamestats.diablo.hero;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroStats {

    @JsonProperty("damage")
    private double damage;

    public double getDamage() {
        return this.damage;
    }

}
