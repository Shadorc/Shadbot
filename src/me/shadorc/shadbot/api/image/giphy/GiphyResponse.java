package me.shadorc.shadbot.api.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GiphyResponse {

    @JsonProperty("data")
    private List<GiphyGif> gifs;

    public List<GiphyGif> getGifs() {
        return this.gifs;
    }

}
