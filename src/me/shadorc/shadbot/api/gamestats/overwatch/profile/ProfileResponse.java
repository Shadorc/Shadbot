package me.shadorc.shadbot.api.gamestats.overwatch.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
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
    private Map<String, String> competitive;

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

    public String getRank() {
        return Objects.requireNonNullElse(this.competitive.get("rank"), "0");
    }

    public String getQuickplayPlaytime() {
        return this.playtime.get("quickplay");
    }

    @Override
    public String toString() {
        return "ProfileResponse{" +
                "message='" + message + '\'' +
                ", username='" + username + '\'' +
                ", level=" + level +
                ", portrait='" + portrait + '\'' +
                ", isPrivate=" + isPrivate +
                ", games=" + games +
                ", playtime=" + playtime +
                ", competitive=" + competitive +
                '}';
    }
}
