package com.shadorc.shadbot.api.image.wallhaven;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Wallpaper {

    @JsonProperty("url")
    private String url;
    @JsonProperty("path")
    private String path;
    @JsonProperty("resolution")
    private String resolution;

    public String getUrl() {
        return this.url;
    }

    public String getPath() {
        return this.path;
    }

    public String getResolution() {
        return this.resolution;
    }
}
