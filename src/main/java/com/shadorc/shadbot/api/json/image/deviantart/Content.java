package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Content {

    @JsonProperty("src")
    private String source;

    public String getSource() {
        return this.source;
    }

}
