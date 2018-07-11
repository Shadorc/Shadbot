package me.shadorc.shadbot.api.wikipedia;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaQuery {

	@JsonProperty("pages")
	private Map<String, WikipediaPage> pages;

	public Map<String, WikipediaPage> getPages() {
		return pages;
	}

	@Override
	public String toString() {
		return "WikipediaQuery [pages=" + pages + "]";
	}

}
