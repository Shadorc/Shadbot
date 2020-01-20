package com.shadorc.shadbot.db.lottery;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LotteryCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.lottery");

    public LotteryCollection(MongoDatabase database) {
        super(database.getCollection("lottery"));
    }

    public Flux<LotteryGambler> getGamblers() {
        LOGGER.debug("[Lottery gamblers] Request.");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "gamblers"))
                .first();

        return Mono.from(request)
                .map(document -> document.getList("gamblers", Document.class))
                .flatMapMany(Flux::fromIterable)
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, LotteryGamblerBean.class)))
                .map(LotteryGambler::new);
    }

    public Mono<LotteryHistoric> getHistoric() {
        LOGGER.debug("[Lottery historic] Request.");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "historic"))
                .first();

        return Mono.from(request)
                .map(Document::toJson)
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, LotteryHistoricBean.class)))
                .map(LotteryHistoric::new);
    }

    public Mono<Long> getJackpot() {
        LOGGER.debug("[Lottery jackpot] Request.");

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", "jackpot"))
                .first();

        return Mono.from(request)
                .map(document -> document.getLong("jackpot"))
                .defaultIfEmpty(0L);
    }

    public Mono<Boolean> isGambler(Snowflake userId) {
        LOGGER.debug("[Gambler {}] Checking if user exist.", userId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.and(Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.user_id", userId.asString())))
                .first();

        return Mono.from(request)
                .hasElement();
    }

    public Mono<DeleteResult> resetGamblers() {
        LOGGER.debug("[Lottery gamblers] Reset.");

        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "gamblers")));
    }

    public Mono<UpdateResult> addToJackpot(long coins) {
        final long value = (long) Math.ceil(coins / 100.0f);

        LOGGER.debug("[Lottery jackpot] Adding {}.", FormatUtils.coins(value));

        return Mono.from(this.getCollection()
                .updateOne(Filters.eq("_id", "jackpot"),
                        Updates.inc("jackpot", value),
                        new UpdateOptions().upsert(true)));
    }

    public Mono<DeleteResult> resetJackpot() {
        LOGGER.debug("[Lottery jackpot] Reset.");

        return Mono.from(this.getCollection()
                .deleteOne(Filters.eq("_id", "jackpot")));
    }

}
