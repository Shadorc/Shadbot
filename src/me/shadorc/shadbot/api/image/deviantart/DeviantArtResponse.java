package me.shadorc.shadbot.api.image.deviantart;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviantArtResponse {

	@JsonProperty("results")
	private List<Image> results;

	public List<Image> getResults() {
		return results;
	}

}
