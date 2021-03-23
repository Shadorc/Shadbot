package com.shadorc.shadbot.api.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expires_in")
    private int expiresIn;

    public String getAccessToken() {
        return this.accessToken;
    }

    public Duration getExpiresIn() {
        return Duration.ofSeconds(this.expiresIn);
    }

}
