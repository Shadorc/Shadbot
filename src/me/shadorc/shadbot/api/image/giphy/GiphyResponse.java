package me.shadorc.shadbot.api.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;

public class GiphyResponse {

    @Nullable
    @JsonProperty("data")
    private List<GiphyGif> gifs;

    public List<GiphyGif> getGifs() {
        return this.gifs;
    }

}
