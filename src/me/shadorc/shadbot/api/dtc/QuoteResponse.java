package me.shadorc.shadbot.api.dtc;

import com.fasterxml.jackson.annotation.JsonProperty;

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

	@Override
	public String toString() {
		return "QuoteResponse [content=" + content
				+ ", id=" + id
				+ "]";
	}

}
