package com.locibot.locibot.api.json.gamestats.diablo.profile;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Optional;

public record ProfileResponse(Optional<String> code,
                              String battleTag,
                              int paragonLevel,
                              int paragonLevelHardcore,
                              int paragonLevelSeason,
                              int paragonLevelSeasonHardcore,
                              String guildName,
                              @JsonProperty("heroes") List<HeroId> heroIds) {

}
