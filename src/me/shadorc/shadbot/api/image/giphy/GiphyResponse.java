package me.shadorc.shadbot.api.image.giphy;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import reactor.util.annotation.Nullable;

public class GiphyResponse {

	@Nullable
	@JsonProperty("data")
	private List<GiphyGif> gifs;

	public List<GiphyGif> getGifs() {
		return this.gifs;
	}

}
