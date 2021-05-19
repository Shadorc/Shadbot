package com.locibot.locibot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.utils.FormatUtil;

import java.util.List;

public record Quickplay(List<HeroPlayed> played,
                        @JsonProperty("eliminations_per_life") List<HeroEliminations> eliminationsPerLife) {

    private static final int RANKING_SIZE = 3;
    private static final String FORMAT = "**%s**. %s (%s)";

    public String getPlayed() {
        return FormatUtil.numberedList(RANKING_SIZE, this.played.size(),
                count -> FORMAT
                        .formatted(count, this.played.get(count - 1).hero(), this.played.get(count - 1).played()));
    }

    public String getEliminationsPerLife() {
        return FormatUtil.numberedList(RANKING_SIZE, this.eliminationsPerLife.size(),
                count -> FORMAT
                        .formatted(count, this.eliminationsPerLife.get(count - 1).hero(),
                                this.eliminationsPerLife.get(count - 1).eliminationsPerLife()));
    }

}
