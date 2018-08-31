package me.shadorc.shadbot.api.dtc;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Quote {

	@JsonProperty("id")
	private String id;
	@JsonProperty("content")
	private String content;

	public String getId() {
		return this.id;
	}

	public String getContent() {
		return this.content;
	}

	@Override
	public String toString() {
		return String.format("Quote [id=%s, content=%s]", this.id, this.content);
	}

}
