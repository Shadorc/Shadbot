package me.shadorc.shadbot.api.gamestats.overwatch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopHeroes {

	@JsonProperty("played")
	private List<HeroPlayed> heroPlayed;
	@JsonProperty("eliminations_per_life")
	private List<HeroEliminations> heroEliminations;

	public List<HeroPlayed> getHeroPlayed() {
		return heroPlayed;
	}

	public List<HeroEliminations> getHeroEliminations() {
		return heroEliminations;
	}

	@Override
	public String toString() {
		return String.format("TopHeroes [heroPlayed=%s, heroEliminations=%s]", heroPlayed, heroEliminations);
	}

}
