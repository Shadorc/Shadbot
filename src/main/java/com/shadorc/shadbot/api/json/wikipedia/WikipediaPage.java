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

    public String getEncodedTitle() {
        return this.title.replace(" ", "_");
    }

    public String getExtract() {
        return this.extract;
    }

    @Override
    public String toString() {
        return "WikipediaPage{" +
                "title='" + this.title + '\'' +
                ", extract='" + this.extract + '\'' +
                '}';
    }
}
