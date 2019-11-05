package com.shadorc.shadbot.db.lottery.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LotteryGamblerBean {

    @JsonProperty("guild_id")
    private long guildId;
    @JsonProperty("user_id")
    private long userId;
    @JsonProperty("number")
    private int number;

    public long getGuildId() {
        return this.guildId;
    }

    public long getUserId() {
        return this.userId;
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    public String toString() {
        return "LotteryGamblerBean{" +
                "guildId=" + this.guildId +
                ", userId=" + this.userId +
                ", number=" + this.number +
                '}';
    }

}
