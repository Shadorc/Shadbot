package com.shadorc.shadbot.db.premium;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.time.Duration;
import java.util.UUID;

import static com.shadorc.shadbot.db.DatabaseManager.DB_REQUEST_COUNTER;

public class PremiumCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtils.getLogger(PremiumCollection.class, LogUtils.Category.DATABASE);
    public static final String NAME = "premium";

    public PremiumCollection(MongoDatabase database) {
        super(database.getCollection(PremiumCollection.NAME));
    }

    /**
     * @param relicId The ID of the {@link Relic} to get.
     * @return The {@link Relic} corresponding to the provided {@code relicId}.
     */
    public Mono<Relic> getRelicById(String relicId) {
        LOGGER.debug("[Relic {}] Request", relicId);

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", relicId))
                .first();

        return Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtils.MAPPER.readValue(json, RelicBean.class)))
                .map(Relic::new)
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels(PremiumCollection.NAME).inc());
    }

    /**
     * @param userId The {@link Snowflake} ID of the {@link User}.
     * @return A {@link Flux} containing the {@link Relic} possessed by an {@link User}.
     */
    public Flux<Relic> getUserRelics(Snowflake userId) {
        LOGGER.debug("[Premium] Request relics for user {}", userId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("user_id", userId.asString()));

        return Flux.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtils.MAPPER.readValue(json, RelicBean.class)))
                .map(Relic::new)
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels(PremiumCollection.NAME).inc());
    }

    /**
     * @param type The {@link RelicType} type of the {@link Relic} to generate.
     * @return The generated {@link Relic} inserted in the database.
     */
    public static Mono<Relic> generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.RELIC_DURATION));
        LOGGER.info("Relic generated. Type: %s, ID: %s", relic.getType(), relic.getId());
        return relic.insert()
                .thenReturn(relic);
    }

    /**
     * Requests to determine if a {@link Guild} or a {@link User} are premium.
     *
     * @param guildId The {@link Snowflake} ID of the {@link Guild} to check.
     * @param userId  The {@link Snowflake} ID of the {@link User} to check.
     * @return {@code true} if the {@link Guild} or the {@link User} is premium, {@code false} otherwise.
     */
    public Mono<Boolean> isPremium(Snowflake guildId, Snowflake userId) {
        LOGGER.debug("[Premium] Check if user {} in guild {} is premium", userId.asLong(), guildId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.or(
                        Filters.eq("user_id", userId.asString()),
                        Filters.eq("guild_id", guildId.asString())));

        return Flux.from(request)
                .hasElements()
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels(PremiumCollection.NAME).inc());
    }

}
