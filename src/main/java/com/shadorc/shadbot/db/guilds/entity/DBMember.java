package com.shadorc.shadbot.db.guilds.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.DBMemberBean;
import com.shadorc.shadbot.db.guilds.entity.achievement.Achievement;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.rest.util.Snowflake;
import org.bson.Document;
import org.bson.conversions.Bson;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Objects;

import static com.shadorc.shadbot.db.DatabaseManager.DB_REQUEST_COUNTER;
import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

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

    public EnumSet<Achievement> getAchievements() {
        return Achievement.of(this.getBean().getAchievements());
    }

    public Mono<UpdateResult> addCoins(long gains) {
        final long coins = NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        // If the new coins amount is equal to the current one, no need to request an update
        if (coins == this.getCoins()) {
            LOGGER.debug("[DBMember {} / {}] Coins update useless, aborting: {} coins",
                    this.getId().asLong(), this.getGuildId().asLong(), coins);
            return Mono.empty();
        }

        LOGGER.debug("[DBMember {} / {}] Coins update: {} coins", this.getId().asLong(), this.getGuildId().asLong(), coins);
        return this.update(Updates.set("members.$.coins", coins), this.toDocument().append("coins", coins))
                .then(Mono.defer(() -> {
                    if (coins >= 1_000_000_000) {
                        return this.unlockAchievement(Achievement.MONEY);
                    }
                    return Mono.empty();
                }));
    }

    public Mono<UpdateResult> unlockAchievement(Achievement achievement) {
        final int achievements = this.getBean().getAchievements() | achievement.getFlag();
        return this.updateAchievement(achievements);
    }

    public Mono<UpdateResult> lockAchievement(Achievement achievement) {
        final int achievements = this.getBean().getAchievements() & ~achievement.getFlag();
        return this.updateAchievement(achievements);
    }

    private Mono<UpdateResult> updateAchievement(int achievements) {
        // If the achievement is already in this state, no need to request an update
        if (this.getBean().getAchievements() == achievements) {
            LOGGER.debug("[DBMember {} / {}] Achievements update useless, aborting: achievements {}",
                    this.getId().asLong(), this.getGuildId().asLong(), achievements);
            return Mono.empty();
        }

        LOGGER.debug("[DBMember {} / {}] Achievements update: achievements {}",
                this.getId().asLong(), this.getGuildId().asLong(), achievements);
        return this.update(Updates.set("members.$.achievements", achievements), this.toDocument().append("achievements", achievements));

    }

    private Mono<UpdateResult> update(Bson update, Document document) {
        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.and(
                                Filters.eq("_id", this.getGuildId().asString()),
                                Filters.eq("members._id", this.getId().asString())),
                        update))
                .doOnNext(result -> LOGGER.trace("[DBMember {} / {}] Update result: {}",
                        this.getId().asLong(), this.getGuildId().asLong(), result))
                .map(UpdateResult::getModifiedCount)
                .flatMap(modifiedCount -> {
                    // Member was not found, insert it
                    if (modifiedCount == 0) {
                        LOGGER.debug("[DBMember {} / {}] Not updated. Upsert member",
                                this.getId().asLong(), this.getGuildId().asLong());
                        return Mono.from(DatabaseManager.getGuilds()
                                .getCollection()
                                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                                        Updates.push("members", document),
                                        new UpdateOptions().upsert(true)))
                                .doOnNext(result -> LOGGER.trace("[DBMember {} / {}] Upsert result: {}",
                                        this.getId().asLong(), this.getGuildId().asLong(), result));
                    }
                    return Mono.empty();
                })
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels("guilds").inc());
    }

    // Note: If one day, a member contains more data than just coins, this method will need to be updated
    public Mono<Void> resetCoins() {
        LOGGER.debug("[DBMember {} / {}] Coins deletion", this.getId().asLong(), this.getGuildId().asLong());
        return this.delete();
    }

    @Override
    public Mono<Void> insert() {
        LOGGER.debug("[DBMember {} / {}] Insertion", this.getId().asLong(), this.getGuildId().asLong());

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.push("members", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[DBMember {} / {}] Insertion result: {}",
                        this.getId().asLong(), this.getGuildId().asLong(), result))
                .then()
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels("guilds").inc());
    }

    @Override
    public Mono<Void> delete() {
        LOGGER.debug("[DBMember {} / {}] Deletion", this.getId().asLong(), this.getGuildId().asLong());

        return Mono.from(DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.pull("members", Filters.eq("_id", this.getId().asString()))))
                .doOnNext(result -> LOGGER.trace("[DBMember {} / {}] Deletion result: {}",
                        this.getId().asLong(), this.getGuildId().asLong(), result))
                .then()
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels("guilds").inc());
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
