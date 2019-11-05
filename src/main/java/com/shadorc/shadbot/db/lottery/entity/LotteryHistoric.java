package com.shadorc.shadbot.db.lottery.entity;

import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;

public class LotteryHistoric implements DatabaseEntity {

    private final long jackpot;
    private final int winnerCount;
    private final int number;

    public LotteryHistoric(LotteryHistoricBean bean) {
        this.jackpot = bean.getJackpot();
        this.winnerCount = bean.getWinnerCount();
        this.number = bean.getNumber();
    }

    public LotteryHistoric(long jackpot, int winnerCount, int number) {
        this.jackpot = jackpot;
        this.winnerCount = winnerCount;
        this.number = number;
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
    public void insert() {
        // TODO
    }

    @Override
    public void delete() {
        // TODO
    }

    @Override
    public String toString() {
        return "LotteryHistoric{" +
                "jackpot=" + this.jackpot +
                ", winnerCount=" + this.winnerCount +
                ", number=" + this.number +
                '}';
    }

}
