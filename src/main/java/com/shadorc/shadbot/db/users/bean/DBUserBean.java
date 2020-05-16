package com.shadorc.shadbot.db.users.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;
import reactor.util.annotation.Nullable;

public class DBUserBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("achievements")
    private Integer achievements;

    public DBUserBean(String id, @Nullable Integer achievements) {
        this.id = id;
        this.achievements = achievements;
    }

    public DBUserBean(String id) {
        this(id, null);
    }

    public DBUserBean() {
    }

    public String getId() {
        return this.id;
    }

    public int getAchievements() {
        return this.achievements == null ? 0 : this.achievements;
    }

    @Override
    public String toString() {
        return "DBUserBean{" +
                "id='" + this.id + '\'' +
                ", achievements=" + this.achievements +
                '}';
    }
}
