package me.shadorc.shadbot.api.dtc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QuoteResponse {

	@JsonProperty("content")
	private String content;
	@JsonProperty("id")
	private String id;

	public String getContent() {
		return content;
	}

	public String getId() {
		return id;
	}

}
