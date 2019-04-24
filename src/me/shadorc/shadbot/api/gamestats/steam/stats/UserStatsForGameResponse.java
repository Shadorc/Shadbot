package me.shadorc.shadbot.api.gamestats.steam.stats;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

public class UserStatsForGameResponse {

    @Nullable
    @JsonProperty("playerstats")
    private PlayerStats playerStats;

    public PlayerStats getPlayerStats() {
        return this.playerStats;
    }

}
