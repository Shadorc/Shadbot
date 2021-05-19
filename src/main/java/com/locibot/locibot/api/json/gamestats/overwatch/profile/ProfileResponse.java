package com.locibot.locibot.api.json.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Optional;

public record ProfileResponse(Optional<String> message,
                              String username,
                              int level,
                              String portrait,
                              @JsonProperty("private") boolean isPrivate,
                              Games games,
                              Map<String, String> playtime,
                              Competitive competitive) {


    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

}
