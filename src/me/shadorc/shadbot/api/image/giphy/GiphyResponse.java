package me.shadorc.shadbot.api.image.giphy;

import java.util.List;

import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GiphyResponse {

	@Nullable
	@JsonProperty("data")
	private List<GifResponse> data;

	public List<GifResponse> getData() {
		return data;
	}

}
