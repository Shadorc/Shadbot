package com.shadorc.shadbot.db.premium;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.User;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.UUID;

public final class PremiumCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.premium");

    public PremiumCollection(MongoDatabase database) {
        super(database.getCollection("premium"));
    }

    /**
     * @param relicId - the ID of the {@link Relic} to get
     * @return The {@link Relic} corresponding to the provided {@code relicId}.
     */
    public Mono<Relic> getRelicById(String relicId) {
        LOGGER.debug("[Relic {}] Request.", relicId);

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", relicId))
                .first();

        return Mono.from(request)
                .map(document -> document.toJson(Utils.JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, RelicBean.class)))
                .map(Relic::new);
    }

    /**
     * @param userId - the {@link Snowflake} ID of the {@link User}
     * @return A {@link Flux} containing the {@link Relic} possessed by an {@link User}.
     */
    public Flux<Relic> getRelicsByUser(Snowflake userId) {
        LOGGER.debug("[Relics by user {}] Request.", userId.asLong());
        return this.getRelicsBy("user_id", userId);
    }

    /**
     * @param guildId - the {@link Snowflake} ID of the {@link Guild}
     * @return A {@link Flux} containing the {@link Relic} possessed by a {@link Guild}.
     */
    public Flux<Relic> getRelicsByGuild(Snowflake guildId) {
        LOGGER.debug("[Relics by guild {}] Request.", guildId.asLong());
        return this.getRelicsBy("guild_id", guildId);
    }

    private Flux<Relic> getRelicsBy(String key, Snowflake id) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq(key, id.asString()));

        return Flux.from(request)
                .map(document -> document.toJson(Utils.JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> Utils.MAPPER.readValue(json, RelicBean.class)))
                .map(Relic::new);
    }

    /**
     * @param type - the {@link RelicType} type of the {@link Relic} to generate.
     * @return The generated {@link Relic} inserted in the database.
     */
    public Mono<Relic> generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.RELIC_DURATION));
        LOGGER.info("Relic generated. Type: %s, ID: %s", relic.getType(), relic.getId());
        return relic.insert()
                .thenReturn(relic);
    }

    /**
     * Requests to determine if a {@link Guild} or a {@link User} are premium.
     *
     * @param guildId - the {@link Snowflake} ID of the {@link Guild} to check
     * @param userId - the {@link Snowflake} ID of the {@link User} to check
     * @return {@code true} if the {@link Guild} or the {@link User} is premium, {@code false} otherwise.
     */
    public Mono<Boolean> isPremium(Snowflake guildId, Snowflake userId) {
        LOGGER.debug("[Is premium {} / {}] Request.", guildId.asLong(), userId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.or(
                        Filters.eq("user_id", userId.asString()),
                        Filters.eq("guild_id", guildId.asString())));

        return Flux.from(request).hasElements();
    }

    /**
     * Requests to determine if a {@link Guild} is premium.
     *
     * @param guildId - the {@link Snowflake} ID of the {@link Guild} to check
     * @return {@code true} if the {@link Guild} is premium, {@code false} otherwise.
     */
    public Mono<Boolean> isGuildPremium(Snowflake guildId) {
        LOGGER.debug("[Is premium {}] Request.", guildId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("guild_id", guildId.asString()));

        return Flux.from(request).hasElements();
    }

}
