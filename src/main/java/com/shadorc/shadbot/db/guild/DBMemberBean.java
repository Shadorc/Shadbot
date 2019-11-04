package com.shadorc.shadbot.db.guild;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DBMemberBean {

    @JsonProperty("id")
    private long id;
    @JsonProperty("coins")
    private int coins;

    public long getId() {
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
