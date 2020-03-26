package com.shadorc.shadbot.db.guilds.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBGuild extends SerializableEntity<DBGuildBean> implements DatabaseEntity {

    public DBGuild(DBGuildBean bean) {
        super(bean);
    }

    public DBGuild(Snowflake id) {
        super(new DBGuildBean(id.asString(), null, null));
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

    /**
     * {@code value} must be serializable or serialized.
     */
    public <T> Mono<UpdateResult> setSetting(Setting setting, T value) {
        LOGGER.debug("[DBGuild {}] Setting update: {}={}", this.getId().asLong(), setting, value);

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.set(String.format("settings.%s", setting), value),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Setting update result: {}", this.getId().asLong(), result));
    }

    public Mono<UpdateResult> removeSetting(Setting setting) {
        LOGGER.debug("[DBGuild {}] Setting deletion: {}", this.getId().asLong(), setting);

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.unset(String.format("settings.%s", setting))))
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Setting deletion result: {}", this.getId().asLong(), result));
    }

    @Override
    public Mono<Void> insert() {
        LOGGER.debug("[DBGuild {}] Insertion", this.getId().asLong());

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Insertion result: {}", this.getId().asLong(), result))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        LOGGER.debug("[DBGuild {}] Deletion", this.getId().asLong());

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .deleteOne(Filters.eq("_id", this.getId().asString())))
                .doOnNext(result -> LOGGER.trace("[DBGuild {}] Deletion result: {}", this.getId().asLong(), result))
                .then();
    }

    @Override
    public String toString() {
        return "DBGuild{" +
                "bean=" + this.getBean() +
                '}';
    }
}
