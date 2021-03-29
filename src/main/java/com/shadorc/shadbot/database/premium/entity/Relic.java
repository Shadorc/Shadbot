package com.shadorc.shadbot.database.premium.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.database.DatabaseEntity;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.SerializableEntity;
import com.shadorc.shadbot.database.premium.RelicType;
import com.shadorc.shadbot.database.premium.bean.RelicBean;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.shadorc.shadbot.database.premium.PremiumCollection.LOGGER;

public class Relic extends SerializableEntity<RelicBean> implements DatabaseEntity {

    public Relic(RelicBean bean) {
        super(bean);
    }

    public Relic(UUID id, RelicType type, Duration duration) {
        super(new RelicBean(id.toString(), type.toString(), duration.toMillis(), null, null, null));
    }

    public String getId() {
        return this.getBean().getId();
    }

    public RelicType getType() {
        return RelicType.valueOf(this.getBean().getType());
    }

    public Duration getDuration() {
        return Duration.ofMillis(this.getBean().getDuration());
    }

    public Optional<Instant> getActivation() {
        return Optional.ofNullable(this.getBean().getActivation())
                .map(Instant::ofEpochMilli);
    }

    public Optional<Snowflake> getUserId() {
        return Optional.ofNullable(this.getBean().getUserId())
                .map(Snowflake::of);
    }

    public Optional<Snowflake> getGuildId() {
        return Optional.ofNullable(this.getBean().getGuildId())
                .map(Snowflake::of);
    }

    public boolean isExpired() {
        return this.getActivation()
                .map(TimeUtil::elapsed)
                .map(elapsedMillis -> elapsedMillis.compareTo(this.getDuration()) > 0)
                .orElse(false);
    }

    public Mono<UpdateResult> activate(Snowflake userId, @Nullable Snowflake guildId) {
        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getId()),
                        Updates.combine(
                                Updates.set("user_id", userId.asString()),
                                Updates.set("guild_id", guildId == null ? null : guildId.asString()),
                                Updates.set("activation", Instant.now().toEpochMilli()))))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Relic {}] Activation", this.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getPremium().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[Relic {}] Activation result; {}", this.getId(), result))
                .doOnTerminate(DatabaseManager.getPremium()::invalidateCache);
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Relic {}] Insertion", this.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getPremium().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[Relic {}] Insertion result: {}", this.getId(), result))
                .doOnTerminate(DatabaseManager.getPremium()::invalidateCache)
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId())))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[Relic {}] Deletion", this.getId());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getPremium().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[Relic {}] Deletion result: {}", this.getId(), result))
                .doOnTerminate(DatabaseManager.getPremium()::invalidateCache)
                .then();
    }

    @Override
    public String toString() {
        return "Relic{" +
                "bean=" + this.getBean() +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final Relic relic = (Relic) obj;
        return Objects.equals(this.getBean().getId(), relic.getBean().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getBean().getId());
    }
}
