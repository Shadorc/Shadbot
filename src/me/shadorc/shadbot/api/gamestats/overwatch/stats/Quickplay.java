package me.shadorc.shadbot.api.gamestats.overwatch.stats;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.shadorc.shadbot.utils.FormatUtils;

public class Quickplay {

	private final static int RANKING_SIZE = 3;
	private final static String FORMAT = "**%s**. %s (%s)";

	@JsonProperty("played")
	private List<HeroPlayed> played;
	@JsonProperty("eliminations_per_life")
	private List<HeroEliminations> eliminationsPerLife;

	public String getPlayed() {
		return FormatUtils.numberedList(RANKING_SIZE, played.size(), count -> String.format(FORMAT,
				count, played.get(count - 1).getHero(), played.get(count - 1).getPlayed()));
	}

	public String getEliminationsPerLife() {
		return FormatUtils.numberedList(RANKING_SIZE, eliminationsPerLife.size(), count -> String.format(FORMAT,
				count, eliminationsPerLife.get(count - 1).getHero(), eliminationsPerLife.get(count - 1).getEliminationsPerLife()));
	}

	@Override
	public String toString() {
		return String.format("Quickplay [played=%s, eliminationsPerLife=%s]", played, eliminationsPerLife);
	}

}
