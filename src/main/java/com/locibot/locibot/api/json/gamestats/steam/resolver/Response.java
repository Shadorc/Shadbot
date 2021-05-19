package com.locibot.locibot.api.json.gamestats.steam.resolver;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

public record Response(@JsonProperty("steamid") Optional<String> steamId) {

}