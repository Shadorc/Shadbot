package com.shadorc.shadbot.db.lottery.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.db.Bean;

public class LotteryHistoricBean implements Bean {

    @JsonProperty("jackpot")
    private long jackpot;
    @JsonProperty("winner_count")
    private int winnerCount;
    @JsonProperty("number")
    private int number;

    public LotteryHistoricBean(long jackpot, int winnerCount, int number) {
        this.jackpot = jackpot;
        this.winnerCount = winnerCount;
        this.number = number;
    }

    public LotteryHistoricBean() {
    }

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
