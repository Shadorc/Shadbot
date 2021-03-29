package com.shadorc.shadbot.database.lottery.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.DatabaseEntity;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.SerializableEntity;
import com.shadorc.shadbot.database.lottery.bean.LotteryHistoricBean;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.database.premium.PremiumCollection.LOGGER;

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
                .doOnNext(result -> LOGGER.trace("[LotteryHistoric] Insertion result: {}", result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[LotteryHistoric] Insertion");
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
                .doOnNext(result -> LOGGER.trace("[LotteryHistoric] Deletion result: {}", result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[LotteryHistoric] Deletion");
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
