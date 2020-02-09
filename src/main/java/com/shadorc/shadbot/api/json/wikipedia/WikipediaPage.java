package com.shadorc.shadbot.api.json.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WikipediaPage {

    @JsonProperty("title")
    private String title;
    @JsonProperty("extract")
    private String extract;

    public String getTitle() {
        return this.title;
    }

    public String getExtract() {
        return this.extract;
    }

}
