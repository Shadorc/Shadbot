package com.locibot.locibot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record UserStatsForGameResponse(@JsonProperty("playerstats") Optional<PlayerStats> playerStats) {

}
