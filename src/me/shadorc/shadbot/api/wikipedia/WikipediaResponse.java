package me.shadorc.shadbot.api.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaResponse {

	@JsonProperty("query")
	private WikipediaQuery query;

	public WikipediaQuery getQuery() {
		return query;
	}

	@Override
	public String toString() {
		return String.format("WikipediaResponse [query=%s]", query);
	}

}
