package com.shadorc.shadbot.db.premium.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.utils.TimeUtil;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.shadorc.shadbot.db.premium.PremiumCollection.LOGGER;

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
                .map(TimeUtil::getMillisUntil)
                .map(elapsedMillis -> elapsedMillis >= this.getDuration().toMillis())
                .orElse(false);
    }

    public Mono<UpdateResult> activate(Snowflake userId, @Nullable Snowflake guildId) {
        LOGGER.debug("[Relic {}] Activation", this.getId());

        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getId()),
                        Updates.combine(
                                Updates.set("user_id", userId.asString()),
                                Updates.set("guild_id", guildId == null ? null : guildId.asString()),
                                Updates.set("activation", Instant.now().toEpochMilli()))))
                .doOnNext(result -> LOGGER.trace("[Relic {}] Activation result; {}", this.getId(), result));
    }

    @Override
    public Mono<Void> insert() {
        LOGGER.debug("[Relic {}] Insertion", this.getId());

        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnNext(result -> LOGGER.trace("[Relic {}] Insertion result: {}", this.getId(), result))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        LOGGER.debug("[Relic {}] Deletion", this.getId());

        return Mono.from(DatabaseManager.getPremium()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId())))
                .doOnNext(result -> LOGGER.trace("[Relic {}] Deletion result: {}", this.getId(), result))
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
