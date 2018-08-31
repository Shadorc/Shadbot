package me.shadorc.shadbot.api.gamestats.overwatch.stats;

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
		return String.format("HeroEliminations [hero=%s, eliminationsPerLife=%s]", this.hero, this.eliminationsPerLife);
	}

}
