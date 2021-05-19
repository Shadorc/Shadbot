package com.locibot.locibot.database.guilds.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.locibot.locibot.database.Bean;
import reactor.util.annotation.Nullable;

public class DBMemberBean implements Bean {

    @JsonProperty("_id")
    private String id;
    @Nullable
    @JsonProperty("coins")
    private Long coins;

    public DBMemberBean(String id, @Nullable Long coins) {
        this.id = id;
        this.coins = coins;
    }

    public DBMemberBean(String id) {
        this(id, null);
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
