package com.shadorc.shadbot.db.lottery.entity;

import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import discord4j.core.object.util.Snowflake;

public class LotteryGambler implements DatabaseEntity {

    private long guildId;
    private long userId;
    private int number;

    public LotteryGambler(LotteryGamblerBean bean) {
        this.guildId = bean.getGuildId();
        this.userId = bean.getUserId();
        this.number = bean.getNumber();
    }

    public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
        this.guildId = guildId.asLong();
        this.userId = userId.asLong();
        this.number = number;
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getUserId() {
        return Snowflake.of(this.userId);
    }

    public int getNumber() {
        return number;
    }

    @Override
    public void insert() {
        // TODO
    }

    @Override
    public void delete() {
        // TODO
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "guildId=" + guildId +
                ", userId=" + userId +
                ", number=" + number +
                '}';
    }

}
