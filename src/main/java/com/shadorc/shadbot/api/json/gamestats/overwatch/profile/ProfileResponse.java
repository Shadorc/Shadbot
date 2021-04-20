package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

public record ProfileResponse(@Nullable String message,
                              String username,
                              int level,
                              String portrait,
                              boolean isPrivate,
                              Games games,
                              Map<String, String> playtime,
                              Competitive competitive) {


    public Optional<String> getMessage() {
        return Optional.ofNullable(this.message);
    }

    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

}
