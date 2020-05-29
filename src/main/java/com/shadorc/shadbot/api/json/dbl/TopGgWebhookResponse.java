package com.shadorc.shadbot.api.json.dbl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TopGgWebhookResponse {

    @JsonProperty("user")
    private String userId;

    public String getUserId() {
        return this.userId;
    }

    @Override
    public String toString() {
        return "WebhookResponse{" +
                "userId='" + this.userId + '\'' +
                '}';
    }
}
