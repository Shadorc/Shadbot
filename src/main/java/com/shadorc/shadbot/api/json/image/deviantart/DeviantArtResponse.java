package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class DeviantArtResponse {

    @JsonProperty("results")
    private List<Image> results;

    public List<Image> getResults() {
        return Collections.unmodifiableList(this.results);
    }

}
