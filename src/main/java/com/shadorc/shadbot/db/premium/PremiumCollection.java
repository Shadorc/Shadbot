package com.shadorc.shadbot.db.premium;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PremiumCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.premium");

    private static final JsonWriterSettings SETTINGS = JsonWriterSettings.builder()
            .int64Converter((value, writer) -> writer.writeNumber(value.toString()))
            .build();

    public PremiumCollection(MongoDatabase database) {
        super(database.getCollection("premium"));
    }

    public Optional<Relic> getRelicById(String relicId) {
        LOGGER.debug("[Relic {}] Request.", relicId);

        final Document document = this.getCollection()
                .find(Filters.eq("_id", relicId))
                .first();

        if (document == null) {
            LOGGER.debug("[Relic {}] Not found.", relicId);
            return Optional.empty();
        } else {
            LOGGER.debug("[Relic {}] Found.", relicId);
            return Optional.of(document)
                    .map(doc -> doc.toJson(SETTINGS))
                    .map(json -> {
                        try {
                            return Utils.MAPPER.readValue(json, RelicBean.class);
                        } catch (final JsonProcessingException err) {
                            throw new RuntimeException(err);
                        }
                    })
                    .map(Relic::new);
        }
    }

    public List<Relic> getRelicsByUser(Snowflake userId) {
        LOGGER.debug("[Relics by user {}] Request.", userId.asLong());
        return this.getRelicsBy("user_id", userId);
    }

    public List<Relic> getRelicsByGuild(Snowflake guildId) {
        LOGGER.debug("[Relics by guild {}] Request.", guildId.asLong());
        return this.getRelicsBy("guild_id", guildId);
    }

    private List<Relic> getRelicsBy(String key, Snowflake id) {
        return Lists.newArrayList(this.getCollection()
                .find(Filters.eq(key, id.asString()))
                .iterator())
                .stream()
                .map(doc -> doc.toJson(SETTINGS))
                .map(json -> {
                    try {
                        return Utils.MAPPER.readValue(json, RelicBean.class);
                    } catch (final JsonProcessingException err) {
                        throw new RuntimeException(err);
                    }
                })
                .map(Relic::new)
                .collect(Collectors.toUnmodifiableList());
    }

    public Relic generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.DEFAULT_RELIC_DURATION));
        relic.insert();
        return relic;
    }

    public boolean isUserPremium(Snowflake userId) {
        return !this.getRelicsByUser(userId).isEmpty();
    }

    public boolean isGuildPremium(Snowflake guildId) {
        return !this.getRelicsByGuild(guildId).isEmpty();
    }

}
