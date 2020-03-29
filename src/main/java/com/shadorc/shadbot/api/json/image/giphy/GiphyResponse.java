package com.shadorc.shadbot.api.json.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class GiphyResponse {

    @JsonProperty("data")
    private List<Data> data;

    public List<Data> getData() {
        return Collections.unmodifiableList(this.data);
    }

}
