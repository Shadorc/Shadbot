package com.shadorc.shadbot.db.premium;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
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

    public Flux<Relic> getRelicsByUser(Snowflake userId) {
        LOGGER.debug("[Relics by user {}] Request.", userId.asLong());
        return this.getRelicsBy("user_id", userId);
    }

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

    public Relic generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.RELIC_DURATION));
        relic.insert();
        return relic;
    }

    public Mono<Boolean> isPremium(Snowflake guildId, Snowflake userId) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.or(
                        Filters.eq("user_id", userId.asString()),
                        Filters.eq("guild_id", guildId.asString())));

        return Flux.from(request).hasElements();
    }

}
