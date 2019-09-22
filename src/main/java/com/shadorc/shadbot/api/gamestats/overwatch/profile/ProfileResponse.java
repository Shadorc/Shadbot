package com.shadorc.shadbot.api.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Optional;

public class ProfileResponse {

    @Nullable
    @JsonProperty("message")
    private String message;
    @JsonProperty("username")
    private String username;
    @JsonProperty("level")
    private int level;
    @JsonProperty("portrait")
    private String portrait;
    @JsonProperty("private")
    private boolean isPrivate;
    @JsonProperty("games")
    private Games games;
    @JsonProperty("playtime")
    private Map<String, String> playtime;
    @JsonProperty("competitive")
    private Competitive competitive;

    public Optional<String> getMessage() {
        return Optional.ofNullable(this.message);
    }

    public String getUsername() {
        return this.username;
    }

    public String getLevel() {
        return Integer.toString(this.level);
    }

    public String getPortrait() {
        return this.portrait;
    }

    public boolean isPrivate() {
        return this.isPrivate;
    }

    public Games getGames() {
        return this.games;
    }

    public String formatCompetitive() {
        final StringBuilder strBuilder = new StringBuilder();
        this.competitive.getDamage().getRank().ifPresent(rank -> strBuilder.append(String.format("%nDamage: %d", rank)));
        this.competitive.getTank().getRank().ifPresent(rank -> strBuilder.append(String.format("%nTank: %d", rank)));
        this.competitive.getSupport().getRank().ifPresent(rank -> strBuilder.append(String.format("%nSupport: %d", rank)));
        return strBuilder.length() == 0 ? "Not ranked" : strBuilder.toString();
    }

    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

}
