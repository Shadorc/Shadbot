package com.shadorc.shadbot.db.users.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.users.UsersCollection;
import com.shadorc.shadbot.db.users.bean.DBUserBean;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import discord4j.common.util.Snowflake;
import org.apache.commons.lang3.NotImplementedException;
import reactor.core.publisher.Mono;

import java.util.EnumSet;
import java.util.Objects;

import static com.shadorc.shadbot.db.DatabaseManager.DB_REQUEST_COUNTER;
import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBUser extends SerializableEntity<DBUserBean> implements DatabaseEntity {

    public DBUser(DBUserBean bean) {
        super(bean);
    }

    public DBUser(Snowflake id) {
        super(new DBUserBean(id.asString()));
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public EnumSet<Achievement> getAchievements() {
        return Achievement.of(this.getBean().getAchievements());
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
            LOGGER.debug("[DBUser {}] Achievements update useless, aborting: {}",
                    this.getId().asLong(), achievements);
            return Mono.empty();
        }

        LOGGER.debug("[DBUser {}] Achievements update: {}", this.getId().asLong(), achievements);

        return Mono.from(DatabaseManager.getUsers()
                .getCollection()
                .updateOne(
                        Filters.eq("_id", this.getId().asString()),
                        Updates.set("achievements", achievements),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[DBUser {}] Achievements update result: {}",
                        this.getId().asLong(), result))
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels(UsersCollection.NAME).inc());
    }

    @Override
    public Mono<Void> insert() {
        throw new NotImplementedException();
    }

    @Override
    public Mono<Void> delete() {
        throw new NotImplementedException();
    }

    @Override
    public String toString() {
        return "DBUser{" +
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
        final DBUser dbUser = (DBUser) obj;
        return Objects.equals(this.getBean().getId(), dbUser.getBean().getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getBean().getId());
    }
}
