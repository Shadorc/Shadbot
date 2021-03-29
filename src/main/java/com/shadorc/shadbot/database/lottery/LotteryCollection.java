package com.shadorc.shadbot.database.lottery;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.core.cache.SingleValueCache;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.DatabaseCollection;
import com.shadorc.shadbot.database.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.database.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.database.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.database.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.List;

public class LotteryCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(LotteryCollection.class, LogUtil.Category.DATABASE);

    private final SingleValueCache<List<LotteryGambler>> gamblersCache;
    private final SingleValueCache<LotteryHistoric> historicCache;
    private final SingleValueCache<Long> jackpotCache;

    public LotteryCollection(MongoDatabase database) {
        super(database, "lottery");
        this.gamblersCache = SingleValueCache.Builder.create(this.requestGamblers()).withInfiniteTtl().build();
        this.historicCache = SingleValueCache.Builder.create(this.requestHistoric()).withInfiniteTtl().build();
        this.jackpotCache = SingleValueCache.Builder.create(this.requestJackpot()).withInfiniteTtl().build();
    }

    public Flux<LotteryGambler> getGamblers() {
        return this.gamblersCache.getOrCache(this.requestGamblers())
                .flatMapMany(Flux::fromIterable);
    }

    private Mono<List<LotteryGambler>> requestGamblers() {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "gamblers"))
                .first();

        return Mono.from(request)
                .flatMapIterable(document -> document.getList("gamblers", Document.class))
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, LotteryGamblerBean.class)))
                .map(LotteryGambler::new)
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Request gamblers");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                })
                .collectList();
    }

    public Mono<LotteryHistoric> getHistoric() {
        return this.historicCache.getOrCache(this.requestHistoric());
    }

    private Mono<LotteryHistoric> requestHistoric() {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "historic"))
                .first();

        return Mono.from(request)
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, LotteryHistoricBean.class)))
                .map(LotteryHistoric::new)
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Request historic");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });
    }

    public Mono<Long> getJackpot() {
        return this.jackpotCache.getOrCache(this.requestJackpot());
    }

    private Mono<Long> requestJackpot() {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "jackpot"))
                .first();

        return Mono.from(request)
                .map(document -> document.getLong("jackpot"))
                .defaultIfEmpty(0L)
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Request jackpot");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });
    }

    public Mono<Boolean> isGambler(Snowflake userId) {
        return this.getGamblers()
                .filter(lotteryGambler -> lotteryGambler.getUserId().equals(userId))
                .hasElements();
    }

    public Mono<DeleteResult> resetGamblers() {
        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "gamblers")))
                .doOnNext(result -> LOGGER.trace("[Lottery] Gamblers deletion result: {}", result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Gamblers deletion");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                })
                .doOnTerminate(this.gamblersCache::invalidate);
    }

    public Mono<UpdateResult> addToJackpot(long coins) {
        final long value = (long) Math.ceil(coins / 100.0f);

        return Mono.from(this.getCollection()
                .updateOne(Filters.eq("_id", "jackpot"),
                        Updates.inc("jackpot", value),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[Lottery] Jackpot update result: {}", result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Jackpot update: {} coins", value);
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                })
                .doOnTerminate(this.jackpotCache::invalidate);
    }

    public Mono<DeleteResult> resetJackpot() {
        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "jackpot")))
                .doOnNext(result -> LOGGER.trace("[Lottery] Jackpot deletion result: {}", result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Lottery] Jackpot deletion");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                })
                .doOnTerminate(this.jackpotCache::invalidate);
    }

    public void invalidateGamblersCache() {
        this.gamblersCache.invalidate();
    }

    public void invalidateHistoricCache() {
        this.historicCache.invalidate();
    }


}
