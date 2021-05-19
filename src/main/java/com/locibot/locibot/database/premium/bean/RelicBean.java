package com.locibot.locibot.database.premium.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class RelicBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("duration")
    private long duration;
    @Nullable
    @JsonProperty("activation")
    private Long activation;
    @Nullable
    @JsonProperty("guild_id")
    private String guildId;
    @Nullable
    @JsonProperty("user_id")
    private String userId;

    public RelicBean(String id, String type, long duration, @Nullable Long activation, @Nullable String guildId, @Nullable String userId) {
        this.id = id;
        this.type = type;
        this.duration = duration;
        this.activation = activation;
        this.guildId = guildId;
        this.userId = userId;
    }

    public RelicBean() {
    }

    public String getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public long getDuration() {
        return this.duration;
    }

    @Nullable
    public Long getActivation() {
        return this.activation;
    }

    @Nullable
    public String getGuildId() {
        return this.guildId;
    }

    @Nullable
    public String getUserId() {
        return this.userId;
    }

    @Override
    public String toString() {
        return "RelicBean{" +
                "id='" + this.id + '\'' +
                ", type='" + this.type + '\'' +
                ", duration=" + this.duration +
                ", activation=" + this.activation +
                ", guildId=" + this.guildId +
                ", userId=" + this.userId +
                '}';
    }

}
