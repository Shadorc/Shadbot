package com.shadorc.shadbot.api.json.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSummariesResponse {

    @JsonProperty("response")
    private PlayerSummaries response;

    public PlayerSummaries getResponse() {
        return this.response;
    }

}
