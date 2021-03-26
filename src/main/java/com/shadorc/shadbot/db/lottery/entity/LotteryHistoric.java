package com.shadorc.shadbot.db.lottery.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.db.premium.PremiumCollection.LOGGER;

public class LotteryHistoric extends SerializableEntity<LotteryHistoricBean> implements DatabaseEntity {

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
    public Mono<Void> insert() {
        LOGGER.debug("[LotteryHistoric] Insertion");

        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .replaceOne(Filters.eq("_id", "historic"),
                        this.toDocument(),
                        new ReplaceOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[LotteryHistoric] Insertion result: {}", result))
                .then()
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc());
    }

    @Override
    public Mono<Void> delete() {
        LOGGER.debug("[LotteryHistoric] Deletion");

        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.eq("_id", "historic")))
                .doOnNext(result -> LOGGER.trace("[LotteryHistoric] Deletion result: {}", result))
                .then()
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc());
    }

    @Override
    public String toString() {
        return "LotteryHistoric{" +
                "bean=" + this.getBean() +
                '}';
    }
}
