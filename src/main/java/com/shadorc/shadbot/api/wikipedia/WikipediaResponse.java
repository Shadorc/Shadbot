package com.shadorc.shadbot.api.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaResponse {

    @JsonProperty("query")
    private WikipediaQuery query;

    public WikipediaQuery getQuery() {
        return this.query;
    }

}
