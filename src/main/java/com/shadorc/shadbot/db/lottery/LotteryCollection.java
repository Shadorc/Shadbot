package com.shadorc.shadbot.db.lottery;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

public class LotteryCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(LotteryCollection.class, LogUtil.Category.DATABASE);

    public LotteryCollection(MongoDatabase database) {
        super(database, "lottery");
    }

    public Flux<LotteryGambler> getGamblers() {
        LOGGER.debug("[Lottery] Request gamblers");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "gamblers"))
                .first();

        return Mono.from(request)
                .flatMapIterable(document -> document.getList("gamblers", Document.class))
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, LotteryGamblerBean.class)))
                .map(LotteryGambler::new)
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<LotteryHistoric> getHistoric() {
        LOGGER.debug("[Lottery] Request historic");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "historic"))
                .first();

        return Mono.from(request)
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, LotteryHistoricBean.class)))
                .map(LotteryHistoric::new)
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<Long> getJackpot() {
        LOGGER.debug("[Lottery] Request jackpot");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "jackpot"))
                .first();

        return Mono.from(request)
                .map(document -> document.getLong("jackpot"))
                .defaultIfEmpty(0L)
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<Boolean> isGambler(Snowflake userId) {
        LOGGER.debug("[Gambler {}] Checking if user exist", userId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.and(Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.user_id", userId.asString())))
                .first();

        return Mono.from(request)
                .hasElement()
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<DeleteResult> resetGamblers() {
        LOGGER.debug("[Lottery] Gamblers deletion");

        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "gamblers")))
                .doOnNext(result -> LOGGER.trace("[Lottery] Gamblers deletion result: {}", result))
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<UpdateResult> addToJackpot(long coins) {
        final long value = (long) Math.ceil(coins / 100.0f);

        LOGGER.debug("[Lottery] Jackpot update: {} coins", value);

        return Mono.from(this.getCollection()
                .updateOne(Filters.eq("_id", "jackpot"),
                        Updates.inc("jackpot", value),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[Lottery] Jackpot update result: {}", result))
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

    public Mono<DeleteResult> resetJackpot() {
        LOGGER.debug("[Lottery] Jackpot deletion");

        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "jackpot")))
                .doOnNext(result -> LOGGER.trace("[Lottery] Jackpot deletion result: {}", result))
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc());
    }

}
