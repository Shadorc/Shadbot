package me.shadorc.shadbot.api.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class WikipediaQuery {

    @JsonProperty("pages")
    private Map<String, WikipediaPage> pages;

    public Map<String, WikipediaPage> getPages() {
        return this.pages;
    }

}
