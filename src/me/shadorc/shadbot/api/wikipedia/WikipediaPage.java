package me.shadorc.shadbot.api.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaPage {

	@JsonProperty("title")
	private String title;
	@JsonProperty("extract")
	private String extract;

	public String getTitle() {
		return title;
	}

	public String getExtract() {
		return extract;
	}

	@Override
	public String toString() {
		return String.format("WikipediaPage [title=%s, extract=%s]", title, extract);
	}

}
