package me.shadorc.shadbot.api.gamestats.overwatch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Game {

	@JsonProperty("quickplay")
	private List<Value> quickplay;
	@JsonProperty("competitive")
	private List<Value> competitive;

	public List<Value> getQuickplay() {
		return quickplay;
	}

	public List<Value> getCompetitive() {
		return competitive;
	}

	@Override
	public String toString() {
		return String.format("Game [quickplay=%s, competitive=%s]", quickplay, competitive);
	}

}
