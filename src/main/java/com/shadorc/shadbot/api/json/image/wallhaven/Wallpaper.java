package com.shadorc.shadbot.api.json.image.wallhaven;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;
import java.util.function.Predicate;

public class Wallpaper {

    @JsonProperty("url")
    private String url;
    @JsonProperty("path")
    private String path;
    @JsonProperty("resolution")
    private String resolution;
    @JsonProperty("purity")
    private String purity;
    @JsonProperty("source")
    private String source;

    public String getUrl() {
        return this.url;
    }

    public String getPath() {
        return this.path;
    }

    public String getResolution() {
        return this.resolution;
    }

    public String getPurity() {
        return this.purity;
    }

    public Optional<String> getSource() {
        return Optional.of(this.source).
                filter(Predicate.not(String::isBlank));
    }

    @Override
    public String toString() {
        return "Wallpaper{" +
                "url='" + this.url + '\'' +
                ", path='" + this.path + '\'' +
                ", resolution='" + this.resolution + '\'' +
                ", purity='" + this.purity + '\'' +
                ", source='" + this.source + '\'' +
                '}';
    }
}
