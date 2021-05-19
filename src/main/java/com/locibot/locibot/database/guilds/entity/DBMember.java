package com.locibot.locibot.database.guilds.entity;

import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.locibot.locibot.database.guilds.bean.DBMemberBean;
import com.locibot.locibot.database.users.entity.achievement.Achievement;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.utils.NumberUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class DBMember extends SerializableEntity<DBMemberBean> implements DatabaseEntity {

    private final String guildId;

    public DBMember(Snowflake guildId, DBMemberBean bean) {
        super(bean);
        this.guildId = guildId.asString();
    }

    public DBMember(Snowflake guildId, Snowflake id) {
        super(new DBMemberBean(id.asString()));
        this.guildId = guildId.asString();
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public long getCoins() {
        return this.getBean().getCoins();
    }

    public Mono<UpdateResult> addCoins(long gains) {
        final long coins = NumberUtil.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        // If the new coins amount is equal to the current one, no need to request an update
        if (coins == this.getCoins()) {
            GuildsCollection.LOGGER.debug("[DBMember {}/{}] Coins update useless, aborting: {} coins",
                    this.getId().asString(), this.getGuildId().asString(), coins);
            return Mono.empty();
        }

        return this.update(Updates.set("members.$.coins", coins), this.toDocument().append("coins", coins))
                .then(Mono.defer(() -> {
                    if (coins >= 1_000_000_000) {
                        return DatabaseManager.getUsers()
                                .getDBUser(this.getId())
                                .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.CROESUS));
                    }
                    if (coins >= 1_000_000) {
                        return DatabaseManager.getUsers()
                                .getDBUser(this.getId())
                                .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.MILLIONAIRE));
                    }
                    return Mono.empty();
                }))
                .doOnSubscribe(__ -> GuildsCollection.LOGGER.debug("[DBMember {}/{}] Coins update: {} coins",
                        this.getId().asString(), this.getGuildId().asString(), coins));
    }

    private Mono<UpdateResult> update(Bson update, Document document) {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.and(
                                Filters.eq("_id", this.getGuildId().asString()),
                                Filters.eq("members._id", this.getId().asString())),
                        update))
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBMember {}/{}] Update result: {}",
                        this.getId().asString(), this.getGuildId().asString(), result))
                .map(UpdateResult::getModifiedCount)
                .flatMap(modifiedCount -> {
                    // Member was not found, insert it
                    if (modifiedCount == 0) {
                        GuildsCollection.LOGGER.debug("[DBMember {}/{}] Not updated. Upsert member",
                                this.getId().asString(), this.getGuildId().asString());
                        return Mono.from(DatabaseManager.getGuilds()
                                .getCollection()
                                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                                        Updates.push("members", document),
                                        new UpdateOptions().upsert(true)))
                                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBMember {}/{}] Upsert result: {}",
                                        this.getId().asString(), this.getGuildId().asString(), result))
                                .doOnSubscribe(__ -> Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc());
                    }
                    return Mono.empty();
                })
                .doOnSubscribe(__ -> Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc())
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getGuildId()));
    }

    // Note: If one day, a member contains more data than just coins, this method will need to be updated
    public Mono<Void> resetCoins() {
        return this.delete()
                .doOnSubscribe(__ -> GuildsCollection.LOGGER.debug("[DBMember {}/{}] Coins deletion",
                        this.getId().asString(), this.getGuildId().asString()));
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.push("members", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBMember {}/{}] Insertion", this.getId().asString(), this.getGuildId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBMember {}/{}] Insertion result: {}",
                        this.getId().asString(), this.getGuildId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getGuildId()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.pull("members", Filters.eq("_id", this.getId().asString()))))
                .doOnSubscribe(__ -> {
                    GuildsCollection.LOGGER.debug("[DBMember {}/{}] Deletion", this.getId().asString(), this.getGuildId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGuilds().getName()).inc();
                })
                .doOnNext(result -> GuildsCollection.LOGGER.trace("[DBMember {}/{}] Deletion result: {}",
                        this.getId().asString(), this.getGuildId().asString(), result))
                .doOnTerminate(() -> DatabaseManager.getGuilds().invalidateCache(this.getGuildId()))
                .then();
    }

    @Override
    public String toString() {
        return "DBMember{" +
                "guildId='" + this.guildId + '\'' +
                ", bean=" + this.getBean() +
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
        final DBMember dbMember = (DBMember) obj;
        return Objects.equals(this.guildId, dbMember.guildId)
                && Objects.equals(this.getBean().getId(), dbMember.getBean().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.guildId, this.getBean().getId());
    }
}
