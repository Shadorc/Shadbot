package com.shadorc.shadbot.api.json.gamestats.overwatch.profile;

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

    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

    public Competitive getCompetitive() {
        return this.competitive;
    }

    @Override
    public String toString() {
        return "ProfileResponse{" +
                "message='" + this.message + '\'' +
                ", username='" + this.username + '\'' +
                ", level=" + this.level +
                ", portrait='" + this.portrait + '\'' +
                ", isPrivate=" + this.isPrivate +
                ", games=" + this.games +
                ", playtime=" + this.playtime +
                ", competitive=" + this.competitive +
                '}';
    }
}
