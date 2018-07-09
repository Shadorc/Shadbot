package me.shadorc.shadbot.api.image.deviantart;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviantArtResponse {

	@JsonProperty("results")
	private List<ResultResponse> results;

	public List<ResultResponse> getResults() {
		return results;
	}

}
