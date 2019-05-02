package me.shadorc.shadbot.api.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

public class Games {

    @Nullable
    @JsonProperty("quickplay")
    private Map<String, Integer> quickplay;

    public Optional<Map<String, Integer>> getQuickplay() {
        return Optional.ofNullable(this.quickplay);
    }

    public String getQuickplayWon() {
        return this.quickplay.get("won").toString();
    }

    @Override
    public String toString() {
        return "Games{" +
                "quickplay=" + quickplay +
                '}';
    }
}
