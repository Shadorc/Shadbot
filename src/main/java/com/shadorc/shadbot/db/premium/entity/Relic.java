package com.shadorc.shadbot.db.premium.entity;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.premium.PremiumManager;
import com.shadorc.shadbot.db.premium.RelicType;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.TimeUtils;
import discord4j.core.object.util.Snowflake;
import reactor.util.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.shadorc.shadbot.db.premium.PremiumManager.LOGGER;

public class Relic implements DatabaseEntity {

    private final String id;
    private final String type;
    private final long duration;
    @Nullable
    private final Long activation;
    @Nullable
    private final Long userId;
    @Nullable
    private final Long guildId;

    public Relic(RelicBean bean) {
        this.id = bean.getId();
        this.type = bean.getType();
        this.duration = bean.getDuration();
        this.activation = bean.getActivation();
        this.userId = bean.getUserId();
        this.guildId = bean.getGuildId();
    }

    public Relic(UUID id, RelicType type, Duration duration) {
        this.id = id.toString();
        this.type = type.toString();
        this.duration = duration.toMillis();
        this.activation = null;
        this.userId = null;
        this.guildId = null;
    }

    public String getId() {
        return this.id;
    }

    public RelicType getType() {
        return RelicType.valueOf(this.type);
    }

    public Duration getDuration() {
        return Duration.ofMillis(this.duration);
    }

    public Optional<Instant> getActivation() {
        return Optional.ofNullable(this.activation)
                .map(Instant::ofEpochMilli);
    }

    public Optional<Snowflake> getUserId() {
        return Optional.ofNullable(this.userId)
                .map(Snowflake::of);
    }

    public Optional<Snowflake> getGuildId() {
        return Optional.ofNullable(this.guildId)
                .map(Snowflake::of);
    }

    public boolean isExpired() {
        return this.getActivation()
                .map(TimeUtils::getMillisUntil)
                .map(elapsedMillis -> elapsedMillis >= this.getDuration().toMillis())
                .orElse(false);
    }

    public void activate(Snowflake userId, @Nullable Snowflake guildId) {
        LOGGER.debug("[Relic {}] Activation...", this.getId());
        try {
            final PremiumManager pm = PremiumManager.getInstance();
            final String response = pm.getTable()
                    .insert(pm.getDatabase().hashMap("id", this.getId())
                            .with("user_id", userId.asLong())
                            .with("guild_id", guildId == null ? null : guildId.asLong())
                            .with("activation", System.currentTimeMillis()))
                    .optArg("conflict", "update")
                    .run(pm.getConnection())
                    .toString();

            LOGGER.debug("[Relic {}] {}", this.getId(), response);

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[Relic %s] An error occurred during activation.", this.getId()));
        }
    }

    @Override
    public void insert() {
        LOGGER.debug("[Relic {}] Inserting...", this.getId());
        try {
            final PremiumManager pm = PremiumManager.getInstance();
            final String response = pm.getTable()
                    .insert(pm.getDatabase()
                            .hashMap("id", this.id)
                            .with("type", this.type)
                            .with("duration", this.duration)
                            .with("activation", this.activation)
                            .with("user_id", this.userId)
                            .with("guild_id", this.guildId))
                    .run(pm.getConnection())
                    .toString();

            LOGGER.debug("[Relic {}] {}", this.getId(), response);

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[Relic %s] An error occurred during insertion.", this.getId()));
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[Relic {}] Deleting...", this.getId());
        try {
            final PremiumManager pm = PremiumManager.getInstance();
            final String response = pm.requestRelic(this.getId())
                    .delete()
                    .run(pm.getConnection())
                    .toString();

            LOGGER.debug("[Relic {}] {}", this.getId(), response);
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[Relic %s] An error occurred during deletion.", this.getId()));
        }
    }

}
