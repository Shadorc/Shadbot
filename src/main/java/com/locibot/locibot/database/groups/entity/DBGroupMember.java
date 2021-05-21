package com.locibot.locibot.database.groups.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.groups.bean.DBGroupMemberBean;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

public class DBGroupMember extends SerializableEntity<DBGroupMemberBean> implements DatabaseEntity {
    private final String groupName;


    public DBGroupMember(DBGroupMemberBean bean, String groupName) {
        super(bean);
        this.groupName = groupName;
    }

    public DBGroupMember(Snowflake id, String groupName) {
        super(new DBGroupMemberBean(id.asLong()));
        this.groupName = groupName;
    }

    public DBGroupMember(Snowflake id, @Nullable String groupName, boolean optional, boolean invited, int accepted, boolean owner) {
        super(new DBGroupMemberBean(id.asLong(), groupName, optional, invited, accepted, owner));
        this.groupName = groupName;
    }

    public String getName() {
        return this.getBean().getName();
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public String getGroupName() {
        return this.groupName;
    }


    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGroupName()),
                        Updates.push("members", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroupMember {}/{}] Insertion", this.getId().asString(), this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGroups().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroupMember {}/{}] Insertion result: {}",
                        this.getId().asString(), this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return null;
    }
}
