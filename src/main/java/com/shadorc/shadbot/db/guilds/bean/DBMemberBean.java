package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

public class DBMemberBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("coins")
    private long coins;

    public DBMemberBean(String id, long coins) {
        this.id = id;
        this.coins = coins;
    }

    public DBMemberBean() {
    }

    public String getId() {
        return this.id;
    }

    public long getCoins() {
        return this.coins;
    }

    @Override
    public String toString() {
        return "DBMemberBean{" +
                "id=" + this.id +
                ", coins=" + this.coins +
                '}';
    }
}
