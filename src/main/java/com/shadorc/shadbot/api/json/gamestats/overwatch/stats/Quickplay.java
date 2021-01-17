package com.shadorc.shadbot.api.json.gamestats.overwatch.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.utils.FormatUtil;

import java.util.List;

public class Quickplay {

    private static final int RANKING_SIZE = 3;
    private static final String FORMAT = "**%s**. %s (%s)";

    @JsonProperty("played")
    private List<HeroPlayed> played;
    @JsonProperty("eliminations_per_life")
    private List<HeroEliminations> eliminationsPerLife;

    public String getPlayed() {
        return FormatUtil.numberedList(RANKING_SIZE, this.played.size(), count -> String.format(FORMAT,
                count, this.played.get(count - 1).getHero(), this.played.get(count - 1).getPlayed()));
    }

    public String getEliminationsPerLife() {
        return FormatUtil.numberedList(RANKING_SIZE, this.eliminationsPerLife.size(), count -> String.format(FORMAT,
                count, this.eliminationsPerLife.get(count - 1).getHero(),
                this.eliminationsPerLife.get(count - 1).getEliminationsPerLife()));
    }

    @Override
    public String toString() {
        return "Quickplay{" +
                "played=" + this.played +
                ", eliminationsPerLife=" + this.eliminationsPerLife +
                '}';
    }
}
