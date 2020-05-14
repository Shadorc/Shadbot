package com.shadorc.shadbot.db.users.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

public class DBUserBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("achievements")
    private Integer achievements;

    public DBUserBean(String id) {
        this.id = id;
    }

    public DBUserBean() {
    }

    public String getId() {
        return this.id;
    }

    public Integer getAchievements() {
        return this.achievements;
    }

    @Override
    public String toString() {
        return "DBUserBean{" +
                "id='" + this.id + '\'' +
                ", achievements=" + this.achievements +
                '}';
    }
}
