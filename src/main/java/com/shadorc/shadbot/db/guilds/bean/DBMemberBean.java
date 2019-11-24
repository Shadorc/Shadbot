package com.shadorc.shadbot.db.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DBMemberBean {

    @JsonProperty("_id")
    private String id;
    @JsonProperty("coins")
    private int coins;

    public DBMemberBean(String id, int coins) {
        this.id = id;
        this.coins = coins;
    }

    public DBMemberBean() {
    }

    public String getId() {
        return this.id;
    }

    public int getCoins() {
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
