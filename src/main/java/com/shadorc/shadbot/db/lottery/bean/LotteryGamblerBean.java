package com.shadorc.shadbot.db.lottery.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LotteryGamblerBean {

    @JsonProperty("guild_id")
    private String guildId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("number")
    private int number;

    public LotteryGamblerBean(String guildId, String userId, int number) {
        this.guildId = guildId;
        this.userId = userId;
        this.number = number;
    }

    public LotteryGamblerBean() {
    }

    public String getGuildId() {
        return this.guildId;
    }

    public String getUserId() {
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
