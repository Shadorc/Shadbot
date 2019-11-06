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
        try {
            LOGGER.debug("[LotteryHistoric] Inserting...");
            final LotteryManager lm = LotteryManager.getInstance();
            lm.getTable()
                    .insert(lm.getDatabase()
                            .hashMap("jackpot", this.jackpot)
                            .with("winner_count", this.winnerCount)
                            .with("number", this.number))
                    .run(LotteryManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "[LotteryHistoric] An error occurred during insertion.");
        }
        LOGGER.debug("[LotteryHistoric] Inserted.");
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[LotteryHistoric] Deleting...");
            LotteryManager.getInstance()
                    .requestHistoric()
                    .delete()
                    .run(LotteryManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "[LotteryHistoric] An error occurred during deletion.");
        }
        LOGGER.debug("[LotteryHistoric] Deleted.");
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
