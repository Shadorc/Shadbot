package com.shadorc.shadbot.api.json.wikipedia;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Map;

public class WikipediaQuery {

    @JsonProperty("pages")
    private Map<String, WikipediaPage> pages;

    public Map<String, WikipediaPage> getPages() {
        return Collections.unmodifiableMap(this.pages);
    }

}
