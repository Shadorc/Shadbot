package me.shadorc.shadbot.api.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GiphyGif {

    @JsonProperty("image_url")
    private String imageUrl;

    public String getImageUrl() {
        return this.imageUrl;
    }

}
