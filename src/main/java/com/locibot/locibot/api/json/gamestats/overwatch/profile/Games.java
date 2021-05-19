package com.locibot.locibot.api.json.gamestats.overwatch.profile;

import java.util.Map;

public record Games(Map<String, Integer> quickplay) {

    public int getQuickplayWon() {
        return this.quickplay.get("won");
    }

}
