package com.locibot.locibot.database.groups;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseCollection;
import com.locibot.locibot.database.groups.bean.DBGroupBean;
import com.locibot.locibot.database.groups.entity.DBGroup;
import com.locibot.locibot.database.groups.entity.DBGroupMember;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GroupsCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(GroupsCollection.class, LogUtil.Category.DATABASE);

    private final MultiValueCache<String, DBGroup> groupCache;


    public GroupsCollection(MongoDatabase database) {
        super(database, "groups");
        this.groupCache = MultiValueCache.Builder.<String, DBGroup>create().withInfiniteTtl().build();

    }

    public List<DBGroup> getAllGroups() {
        List<DBGroup> dbGroups = new ArrayList<>();
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null)
            documents.forEach(document -> {
                try {
                    dbGroups.add(new DBGroup(NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBGroupBean.class)));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        return dbGroups;
    }

    public boolean containsGroup(String groupName) {
        List<Document> documents = Flux.from(this.getCollection().find()).collectList().block();
        if (documents != null) {
            for (Document document : documents) {
                try {
                    if (NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBGroupBean.class).getGroupName().equals(groupName))
                        return true;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }


    public Mono<DBGroup> getDBGroup(String groupName) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", groupName))
                .first();

        final Mono<DBGroup> getDBGroup = Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBGroupBean.class)))
                .map(DBGroup::new)
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBGuild {}] Not found", groupName);
                    }
                })
                .defaultIfEmpty(new DBGroup(groupName))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBGuild {}] Request", groupName);
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });

        return this.groupCache.getOrCache(groupName, getDBGroup);
    }

    public Flux<DBGroupMember> getDBGroupMembers(String groupName, Snowflake... memberIds) {
        return this.getDBGroup(groupName)
                .flatMapIterable(DBGroup::getMembers)
                .collectMap(DBGroupMember::getId)
                .flatMapIterable(dbMembers -> {
                    // Completes the Flux with missing members from the provided array
                    final Set<DBGroupMember> members = new HashSet<>();
                    for (final Snowflake memberId : memberIds) {
                        final DBGroupMember member = dbMembers.getOrDefault(memberId, new DBGroupMember(memberId, groupName));
                        members.add(member);
                    }
                    return members;
                });
    }

    public Mono<DBGroupMember> getDBGroupMember(String groupName, Snowflake memberId) {
        return this.getDBGroupMembers(groupName, memberId)
                .filter(dbMember -> dbMember.getId().equals(memberId))
                .single();
    }

    public void invalidateCache(String groupName) {
        LOGGER.trace("{Guild ID: {}} Cache invalidated", groupName);
        this.groupCache.remove(groupName);
    }
}
