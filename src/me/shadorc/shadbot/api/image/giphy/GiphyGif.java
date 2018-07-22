package me.shadorc.shadbot.api.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GiphyGif {

	@JsonProperty("image_url")
	private String imageUrl;

	public String getImageUrl() {
		return imageUrl;
	}

	@Override
	public String toString() {
		return String.format("GiphyGif [imageUrl=%s]", imageUrl);
	}

}
