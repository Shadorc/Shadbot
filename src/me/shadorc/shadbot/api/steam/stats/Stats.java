package me.shadorc.shadbot.api.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Stats {

	@JsonProperty("name")
	private String name;
	@JsonProperty("value")
	private int value;

	public String getName() {
		return name;
	}

	public int getValue() {
		return value;
	}

}
