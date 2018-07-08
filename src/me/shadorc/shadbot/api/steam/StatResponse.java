package me.shadorc.shadbot.api.steam;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatResponse {

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
