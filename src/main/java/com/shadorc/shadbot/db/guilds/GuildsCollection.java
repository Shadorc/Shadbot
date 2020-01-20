package com.shadorc.shadbot.db.guilds;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.DBMember;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

public class GuildsCollection extends DatabaseCollection {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.guilds");

    public GuildsCollection(MongoDatabase database) {
        super(database.getCollection("guilds"));
    }

    public Mono<DBGuild> getDBGuild(Snowflake guildId) {
        LOGGER.debug("[DBGuild {}] Request", guildId.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", guildId.asString()))
                .first();

        return Mono.from(request)
                .flatMap(document -> Mono.fromCallable(() ->
                        new DBGuild(Utils.MAPPER.readValue(document.toJson(Utils.JSON_WRITER_SETTINGS), DBGuildBean.class))))
                .doOnSuccess(consumer -> {
                    if(consumer == null) {
                        LOGGER.debug("[DBGuild {}] Not found.", guildId.asLong());
                    }
                })
                .defaultIfEmpty(new DBGuild(guildId));
    }

    public Mono<DBMember> getDBMember(Snowflake guildId, Snowflake memberId) {
        LOGGER.debug("[DBMember {} / {}] Request.", memberId.asLong(), guildId.asLong());

        return this.getDBGuild(guildId)
                .map(DBGuild::getMembers)
                .flatMapMany(Flux::fromIterable)
                .filter(dbMember -> dbMember.getId().equals(memberId))
                .next()
                .doOnSuccess(consumer -> {
                    if(consumer == null) {
                        LOGGER.debug("[DBMember {} / {}] Not found.", memberId.asLong(), guildId.asLong());
                    }
                })
                .defaultIfEmpty(new DBMember(guildId, memberId));
    }

}
