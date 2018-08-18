package me.shadorc.shadbot.api.gamestats.overwatch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Value {

	@JsonProperty("title")
	private String title;
	@JsonProperty("value")
	private String value;

	public String getTitle() {
		return title;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return String.format("Value [title=%s, value=%s]", title, value);
	}

}
