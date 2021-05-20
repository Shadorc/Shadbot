package com.locibot.locibot.database.groups.entity;

import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseEntity;
import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.database.SerializableEntity;
import com.locibot.locibot.database.groups.GroupsCollection;
import com.locibot.locibot.database.groups.bean.DBGroupBean;
import com.locibot.locibot.database.guilds.GuildsCollection;
import com.locibot.locibot.database.guilds.bean.DBGuildBean;
import com.locibot.locibot.database.guilds.entity.DBMember;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

public class DBGroup extends SerializableEntity<DBGroupBean> implements DatabaseEntity {

    public DBGroup (DBGroupBean groupBean){
        super(groupBean);
    }

    public DBGroup (String groupName){
        super(new DBGroupBean(groupName));
    }

    public DBGroup (String groupName, int groupType){
        super(new DBGroupBean(groupName, groupType));
    }

    public String getGroupName(){
        return this.getBean().getGroupName();
    }

    public List<DBGroupMember> getMembers() {
        if (this.getBean().getMembers() == null) {
            return Collections.emptyList();
        }

        return this.getBean()
                .getMembers()
                .stream()
                .map(memberBean -> new DBGroupMember(memberBean, getGroupName()))
                .toList();
    }

    @Override
    public Mono<Void> insert() {
        return Mono.from(DatabaseManager.getGroups()
                .getCollection()
                .insertOne(this.toDocument()))
                .doOnSubscribe(__ -> {
                    GroupsCollection.LOGGER.debug("[DBGroup {}] Insertion", this.getGroupName());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getGroups().getName()).inc();
                })
                .doOnNext(result -> GroupsCollection.LOGGER.trace("[DBGroup {}] Insertion result: {}",
                        this.getGroupName(), result))
                .doOnTerminate(() -> DatabaseManager.getGroups().invalidateCache(this.getGroupName()))
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return null;
    }
}
