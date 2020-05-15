package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;
import reactor.util.annotation.Nullable;

public class DBMemberBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("coins")
    private Long coins;

    public DBMemberBean(String id) {
        this.id = id;
    }

    public DBMemberBean() {
    }

    public String getId() {
        return this.id;
    }

    public long getCoins() {
        return this.coins == null ? 0 : this.coins;
    }

    @Override
    public String toString() {
        return "DBMemberBean{" +
                "id=" + this.id +
                ", coins=" + this.coins +
                '}';
    }
}
