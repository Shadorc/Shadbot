package com.shadorc.shadbot.api.json.gamestats.fortnite;

import reactor.util.annotation.Nullable;

import java.util.Optional;

public record FortniteResponse(@Nullable String error,
                               Stats stats) {


    public Optional<String> getError() {
        return Optional.ofNullable(this.error);
    }

}
