package com.shadorc.shadbot.api.json.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Data {

    @JsonProperty("images")
    private Images images;

    public Images getImages() {
        return this.images;
    }

    @Override
    public String toString() {
        return "GiphyGif{" +
                "images=" + this.images +
                '}';
    }
}
