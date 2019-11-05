package com.shadorc.shadbot.db.premium;

import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.util.annotation.Nullable;

public class RelicBean {

    @JsonProperty("id")
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
    private Long guildId;
    @Nullable
    @JsonProperty("user_id")
    private Long userId;

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
    public Long getGuildId() {
        return this.guildId;
    }

    @Nullable
    public Long getUserId() {
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
