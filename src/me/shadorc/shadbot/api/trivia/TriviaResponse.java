package me.shadorc.shadbot.api.trivia;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaResponse {

	@JsonProperty("response_code")
	private int responseCode;
	@JsonProperty("results")
	private List<TriviaResult> results;

	public int getResponseCode() {
		return this.responseCode;
	}

	public List<TriviaResult> getResults() {
		return this.results;
	}

}
