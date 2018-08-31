package me.shadorc.shadbot.api.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaPage {

	@JsonProperty("title")
	private String title;
	@JsonProperty("extract")
	private String extract;

	public String getTitle() {
		return this.title;
	}

	public String getExtract() {
		return this.extract;
	}

	@Override
	public String toString() {
		return String.format("WikipediaPage [title=%s, extract=%s]", this.title, this.extract);
	}

}
