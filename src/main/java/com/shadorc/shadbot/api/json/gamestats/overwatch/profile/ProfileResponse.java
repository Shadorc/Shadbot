package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import java.util.Map;
import java.util.Optional;

public record ProfileResponse(Optional<String> message,
                              String username,
                              int level,
                              String portrait,
                              boolean isPrivate,
                              Games games,
                              Map<String, String> playtime,
                              Competitive competitive) {


    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

}
