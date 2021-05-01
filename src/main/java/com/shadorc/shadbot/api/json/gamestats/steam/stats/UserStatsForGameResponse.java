package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record UserStatsForGameResponse(@JsonProperty("playerstats") Optional<PlayerStats> playerStats) {

}
