package com.shadorc.shadbot.api.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ResolveVanityUrlResponse {

    @JsonProperty("response")
    private Response response;

    public Response getResponse() {
        return this.response;
    }

}
