package com.shadorc.shadbot.api.json.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Original {

    @JsonProperty("url")
    private String url;

    public String getUrl() {
        return this.url;
    }

    @Override
    public String toString() {
        return "Preview{" +
                "mp4Url='" + this.url + '\'' +
                '}';
    }
}
