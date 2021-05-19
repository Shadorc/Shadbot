package com.locibot.locibot.database.premium;

import com.locibot.locibot.database.DatabaseCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.locibot.locibot.core.cache.SingleValueCache;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.premium.bean.RelicBean;
import com.locibot.locibot.database.premium.entity.Relic;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public class PremiumCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(PremiumCollection.class, LogUtil.Category.DATABASE);

    private final SingleValueCache<List<Relic>> relicCache;

    public PremiumCollection(MongoDatabase database) {
        super(database, "premium");
        this.relicCache = SingleValueCache.Builder.create(this.requestRelics()).withInfiniteTtl().build();
    }

    private Mono<List<Relic>> requestRelics() {
        final Publisher<Document> request = this.getCollection()
                .find();

        return Flux.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, RelicBean.class)))
                .map(Relic::new)
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Relic] Request collection");
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                })
                .collectList();
    }

    public Flux<Relic> getRelics() {
        return this.relicCache.getOrCache(this.requestRelics())
                .flatMapMany(Flux::fromIterable);
    }

    /**
     * @param relicId The ID of the {@link Relic} to get.
     * @return The {@link Relic} corresponding to the provided {@code relicId}.
     */
    public Mono<Relic> getRelicById(String relicId) {
        return this.getRelics()
                .filter(relic -> relic.getId().equals(relicId))
                .next();
    }

    /**
     * @param userId The {@link Snowflake} ID of the {@link User}.
     * @return A {@link Flux} containing the {@link Relic} possessed by a {@link User}.
     */
    public Flux<Relic> getUserRelics(Snowflake userId) {
        return this.getRelics()
                .filter(relic -> relic.getUserId().map(userId::equals).orElse(false));
    }

    /**
     * Requests to determine if a {@link Guild} or a {@link User} is premium.
     *
     * @param guildId The {@link Snowflake} ID of the {@link Guild} to check.
     * @param userId  The {@link Snowflake} ID of the {@link User} to check.
     * @return {@code true} if the {@link Guild} or the {@link User} is premium, {@code false} otherwise.
     */
    public Mono<Boolean> isPremium(Snowflake guildId, Snowflake userId) {
        return this.getRelics()
                .filter(relic -> relic.getUserId().map(userId::equals).orElse(false)
                        || relic.getGuildId().map(guildId::equals).orElse(false))
                .hasElements();
    }

    /**
     * @param type The {@link RelicType} type of the {@link Relic} to generate.
     * @return The generated {@link Relic} inserted in the database.
     */
    public static Mono<Relic> generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.RELIC_DURATION));
        return relic.insert()
                .thenReturn(relic)
                .doOnSubscribe(__ -> LOGGER.info("Relic generated. Type: {}, ID: {}", relic.getType(), relic.getId()));
    }

    public void invalidateCache() {
        LOGGER.trace("Cache invalidated");
        this.relicCache.invalidate();
    }

}
