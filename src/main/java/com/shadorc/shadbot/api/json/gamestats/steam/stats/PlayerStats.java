package com.shadorc.shadbot.api.json.gamestats.steam.stats;

import java.util.List;
import java.util.Optional;

public record PlayerStats(Optional<List<Stats>> stats) {

}
