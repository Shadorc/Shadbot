package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HeroEliminations(String hero,
                               @JsonProperty("eliminations_per_life") String eliminationsPerLife) {

}
