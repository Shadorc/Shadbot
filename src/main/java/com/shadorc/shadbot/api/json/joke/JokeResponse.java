package com.shadorc.shadbot.api.json.joke;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JokeResponse {

    @JsonProperty("joke")
    private String joke;

    public String getJoke() {
        return this.joke;
    }

}
