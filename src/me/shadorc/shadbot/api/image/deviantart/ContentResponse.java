package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ContentResponse {

	@JsonProperty("src")
	private String source;

	public String getSource() {
		return source;
	}

}
