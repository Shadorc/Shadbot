package com.shadorc.shadbot.db.lottery.entity;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.lottery.LotteryManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.utils.LogUtils;

import static com.shadorc.shadbot.db.premium.PremiumManager.LOGGER;

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
        LOGGER.debug("[LotteryHistoric] Inserting...");
        try {
            final LotteryManager lm = LotteryManager.getInstance();
            final String response = lm.getTable()
                    .insert(lm.getDatabase().hashMap("id", "historic")
                            .with("jackpot", this.jackpot)
                            .with("winner_count", this.winnerCount)
                            .with("number", this.number))
                    .optArg("conflict", "replace")
                    .run(lm.getConnection())
                    .toString();

            LOGGER.debug("[LotteryHistoric] {}", response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err, "[LotteryHistoric] An error occurred during insertion.");
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[LotteryHistoric] Deleting...");
        try {
            final LotteryManager lm = LotteryManager.getInstance();
            final String response = lm.getTable()
                    .get("historic")
                    .delete()
                    .run(lm.getConnection());

            LOGGER.debug("[LotteryHistoric] {}", response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err, "[LotteryHistoric] An error occurred during deletion.");
        }
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
