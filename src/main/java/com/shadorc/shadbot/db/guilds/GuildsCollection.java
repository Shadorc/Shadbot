package com.shadorc.shadbot.db.guilds;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GuildsCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtils.getLogger(GuildsCollection.class, LogUtils.Category.DATABASE);
    public static final String NAME = "guilds";

    public GuildsCollection(MongoDatabase database) {
        super(database.getCollection(GuildsCollection.NAME));
    }

    public Mono<DBGuild> getDBGuild(Snowflake guildId) {
        LOGGER.debug("[DBGuild {}] Request", guildId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", guildId.asString()))
                .first();

        return Mono.from(request)
                .flatMap(document -> Mono.fromCallable(() ->
                        new DBGuild(NetUtils.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBGuildBean.class))))
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBGuild {}] Not found", guildId.asLong());
                    }
                })
                .defaultIfEmpty(new DBGuild(guildId))
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(GuildsCollection.NAME).inc());
    }

    public Mono<Settings> getSettings(Snowflake guildId) {
        return this.getDBGuild(guildId)
                .map(DBGuild::getSettings);
    }

    public Mono<DBMember> getDBMember(Snowflake guildId, Snowflake memberId) {
        return this.getDBMembers(guildId, memberId)
                .filter(dbMember -> dbMember.getId().equals(memberId))
                .single();
    }

    public Flux<DBMember> getDBMembers(Snowflake guildId, Snowflake... memberIds) {
        LOGGER.debug("[DBMember {} / {}] Request", Arrays.toString(memberIds), guildId.asLong());

        return this.getDBGuild(guildId)
                .flatMapIterable(DBGuild::getMembers)
                .collectMap(DBMember::getId)
                .flatMapIterable(dbMembers -> {
                    final Set<DBMember> members = new HashSet<>();
                    for (final Snowflake memberId : memberIds) {
                        members.add(dbMembers.getOrDefault(memberId, new DBMember(guildId, memberId)));
                    }
                    return members;
                })
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(GuildsCollection.NAME).inc());
    }

}
