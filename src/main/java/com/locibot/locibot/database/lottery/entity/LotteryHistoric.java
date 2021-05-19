package com.locibot.locibot.database.lottery.entity;

import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.lottery.bean.LotteryHistoricBean;
import com.locibot.locibot.database.premium.PremiumCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.locibot.locibot.data.Telemetry;
import reactor.core.publisher.Mono;

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
        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .replaceOne(Filters.eq("_id", "historic"),
                        this.toDocument(),
                        new ReplaceOptions().upsert(true)))
                .doOnNext(result -> PremiumCollection.LOGGER.trace("[LotteryHistoric] Insertion result: {}", result))
                .doOnSubscribe(__ -> {
                    PremiumCollection.LOGGER.debug("[LotteryHistoric] Insertion");
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc();
                })
                .doOnTerminate(DatabaseManager.getLottery()::invalidateHistoricCache)
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.eq("_id", "historic")))
                .doOnNext(result -> PremiumCollection.LOGGER.trace("[LotteryHistoric] Deletion result: {}", result))
                .doOnSubscribe(__ -> {
                    PremiumCollection.LOGGER.debug("[LotteryHistoric] Deletion");
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc();
                })
                .doOnTerminate(DatabaseManager.getLottery()::invalidateHistoricCache)
                .then();
    }

    @Override
    public String toString() {
        return "LotteryHistoric{" +
                "bean=" + this.getBean() +
                '}';
    }
}
