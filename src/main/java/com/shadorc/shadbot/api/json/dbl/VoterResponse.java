package com.shadorc.shadbot.api.json.dbl;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VoterResponse {

    @JsonProperty("id")
    private String id;

    public String getId() {
        return this.id;
    }

    @Override
    public String toString() {
        return "VoterResponse{" +
                "id='" + this.id + '\'' +
                '}';
    }
}
