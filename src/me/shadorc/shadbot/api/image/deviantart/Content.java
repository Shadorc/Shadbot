package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Content {

	@JsonProperty("src")
	private String source;

	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		return String.format("Content [source=%s]", source);
	}

}
