package me.shadorc.shadbot.api.trivia;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriviaResponse {

	@JsonProperty("response_code")
	private int responseCode;
	@JsonProperty("results")
	private List<TriviaResult> results;

	public int getResponseCode() {
		return responseCode;
	}

	public List<TriviaResult> getResults() {
		return results;
	}

	@Override
	public String toString() {
		return String.format("TriviaResponse [responseCode=%s, results=%s]", responseCode, results);
	}

}
