package com.shadorc.shadbot.db.lottery.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;

import static com.shadorc.shadbot.db.premium.PremiumCollection.LOGGER;

public class LotteryHistoric extends DatabaseEntity<LotteryHistoricBean> {

    public LotteryHistoric(LotteryHistoricBean bean) {
        super(bean);
    }

    public LotteryHistoric(long jackpot, int winnerCount, int number) {
        super(new LotteryHistoricBean(jackpot, winnerCount, number));
    }

    public long getJackpot() {
        return this.getBean().getJackpot();
    }

    public int getWinnerCount() {
        return this.getBean().getWinnerCount();
    }

    public int getNumber() {
        return this.getBean().getNumber();
    }

    @Override
    public void insert() {
        LOGGER.debug("[LotteryHistoric] Insertion");

        DatabaseManager.getLottery()
                .getCollection()
                .replaceOne(Filters.eq("_id", "historic"),
                        this.toDocument(),
                        new ReplaceOptions().upsert(true));
    }

    @Override
    public void delete() {
        LOGGER.debug("[LotteryHistoric] Deletion");

        DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.eq("_id", "historic"));
    }

    @Override
    public String toString() {
        return "LotteryHistoric{" +
                "bean=" + this.getBean() +
                '}';
    }
}
