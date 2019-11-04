package com.shadorc.shadbot.db.premium.bean;

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

    @Override
    public String toString() {
        return "BaseRelic{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", duration=" + duration +
                ", activation=" + activation +
                '}';
    }
}
