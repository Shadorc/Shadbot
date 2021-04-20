package com.shadorc.shadbot.api.json.gamestats.diablo.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public record ProfileResponse(@Nullable String code,
                              String battleTag,
                              int paragonLevel,
                              int paragonLevelHardcore,
                              int paragonLevelSeason,
                              int paragonLevelSeasonHardcore,
                              String guildName,
                              @JsonProperty("heroes") List<HeroId> heroIds) {

    public Optional<String> getCode() {
        return Optional.ofNullable(this.code);
    }

}
