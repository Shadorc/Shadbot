package com.shadorc.shadbot.api.json.image.giphy;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Images {

    @JsonProperty("original")
    private Original original;

    public Original getOriginal() {
        return this.original;
    }

    @Override
    public String toString() {
        return "Images{" +
                "preview=" + this.original +
                '}';
    }
}
