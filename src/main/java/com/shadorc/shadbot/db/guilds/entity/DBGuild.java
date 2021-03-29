package com.shadorc.shadbot.db.guilds.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBGuild extends SerializableEntity<DBGuildBean> implements DatabaseEntity {

    public DBGuild(DBGuildBean bean) {
        super(bean);
    }

    public DBGuild(Snowflake id) {
        super(new DBGuildBean(id.asString()));
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public List<DBMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean()
                .getMembers()
                .stream()
                .map(memberBean -> new DBMember(this.getId(), memberBean))
                .collect(Collectors.toUnmodifiableList());
    }

    public Settings getSettings() {
        return new Settings(this.getBean().getSettingsBean());
    }

    public Locale getLocale() {
        return Optional.ofNullable(this.getBean().getLocale())
                .map(Locale::forLanguageTag)
                .orElse(Locale.FRENCH);
        // TODO .orElse(Config.DEFAULT_LOCALE);
    }

    /**
     * {@code value} must be serializable or serialized.
     */
    public <T> Mono<UpdateResult> updateSetting(Setting setting, T value) {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.set("settings.%s".formatted(setting), value),
                        new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Setting update: {}={}", this.getId().asString(), setting, value);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Setting update result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getId()));
    }

    public Mono<UpdateResult> removeSetting(Setting setting) {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.unset(String.format("settings.%s", setting))))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Setting deletion: {}", this.getId().asString(), setting);
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Setting deletion result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getId()));
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Insertion", this.getId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Insertion result: {}",
                        this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getId()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId().asString())))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Deletion", this.getId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Deletion result: {}", this.getId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getId()))
                .then();
    }

    @Override
    public String toString() {
        return "DBGuild{" +
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
        final DBGuild dbGuild = (DBGuild) obj;
        return Objects.equals(this.getBean().getId(), dbGuild.getBean().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getBean().getId());
    }
}
