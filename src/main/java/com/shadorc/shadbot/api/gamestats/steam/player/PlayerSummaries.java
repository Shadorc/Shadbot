package com.shadorc.shadbot.api.gamestats.steam.player;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class PlayerSummaries {

    @JsonProperty("players")
    private List<PlayerSummary> players;

    public List<PlayerSummary> getPlayers() {
        return Collections.unmodifiableList(this.players);
    }

}
