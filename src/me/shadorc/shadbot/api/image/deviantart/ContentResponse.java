package me.shadorc.shadbot.api.image.deviantart;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentResponse {

	@JsonProperty("src")
	private String source;

	public String getSource() {
		return source;
	}

}
