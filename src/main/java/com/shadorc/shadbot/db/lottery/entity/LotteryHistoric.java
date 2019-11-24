package com.shadorc.shadbot.db.lottery.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.utils.Utils;
import org.bson.Document;

import static com.shadorc.shadbot.db.premium.PremiumCollection.LOGGER;

public class LotteryHistoric implements DatabaseEntity {

    private final LotteryHistoricBean bean;

    public LotteryHistoric(LotteryHistoricBean bean) {
        this.bean = bean;
    }

    public LotteryHistoric(long jackpot, int winnerCount, int number) {
        this.bean = new LotteryHistoricBean(jackpot, winnerCount, number);
    }

    public long getJackpot() {
        return this.bean.getJackpot();
    }

    public int getWinnerCount() {
        return this.bean.getWinnerCount();
    }

    public int getNumber() {
        return this.bean.getNumber();
    }

    @Override
    public void insert() {
        LOGGER.debug("[LotteryHistoric] Insertion");

        try {
            DatabaseManager.getLottery()
                    .getCollection()
                    .replaceOne(Filters.eq("_id", "historic"),
                            this.toDocument(),
                            new ReplaceOptions().upsert(true));
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[LotteryHistoric] Deletion");

        DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.eq("_id", "historic"));
    }

    @Override
    public Document toDocument() throws JsonProcessingException {
        return Document.parse(Utils.MAPPER.writeValueAsString(this.bean));
    }

    @Override
    public String toString() {
        return "LotteryHistoric{" +
                "bean=" + this.bean +
                '}';
    }
}
