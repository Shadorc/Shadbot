package com.shadorc.shadbot.api.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Duration;

public record TokenResponse(@JsonProperty("access_token") String accessToken,
                            @JsonProperty("expires_in") int expiresIn) {

    public Duration getExpiresIn() {
        return Duration.ofSeconds(this.expiresIn);
    }

}
