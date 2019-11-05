package com.shadorc.shadbot.db.lottery.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LotteryHistoricBean {

    @JsonProperty("jackpot")
    private long jackpot;
    @JsonProperty("winner_count")
    private int winnerCount;
    @JsonProperty("number")
    private int number;

    public long getJackpot() {
        return this.jackpot;
    }

    public int getWinnerCount() {
        return this.winnerCount;
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    public String toString() {
        return "LotteryHistoricBean{" +
                "jackpot=" + this.jackpot +
                ", winnerCount=" + this.winnerCount +
                ", number=" + this.number +
                '}';
    }
}
