package me.shadorc.shadbot.api.gamestats.overwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HeroPlayed {

	@JsonProperty("hero")
	private String hero;
	@JsonProperty("played")
	private String played;

	public String getHero() {
		return hero;
	}

	public String getPlayed() {
		return played;
	}

	@Override
	public String toString() {
		return String.format("HeroPlayed [hero=%s, played=%s]", hero, played);
	}

}
