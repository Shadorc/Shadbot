package com.shadorc.shadbot.api.json.image.deviantart;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Author {

    @JsonProperty("username")
    private String username;

    public String getUsername() {
        return this.username;
    }

    @Override
    public String toString() {
        return "Author{" +
                "username='" + this.username + '\'' +
                '}';
    }
}
