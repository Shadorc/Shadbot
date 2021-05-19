package com.locibot.locibot.database.users;

import com.locibot.locibot.database.DatabaseCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.locibot.locibot.core.cache.MultiValueCache;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.users.bean.DBUserBean;
import com.locibot.locibot.database.users.entity.DBUser;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

public class UsersCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(UsersCollection.class, LogUtil.Category.DATABASE);

    private final MultiValueCache<Snowflake, DBUser> usersCache;

    public UsersCollection(MongoDatabase database) {
        super(database, "users");
        this.usersCache = MultiValueCache.Builder.<Snowflake, DBUser>create().withInfiniteTtl().build();
    }

    public Mono<DBUser> getDBUser(Snowflake userId) {
        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", userId.asString()))
                .first();

        final Mono<DBUser> getDBUser = Mono.from(request)
                .map(document -> document.toJson(JSON_WRITER_SETTINGS))
                .flatMap(json -> Mono.fromCallable(() -> NetUtil.MAPPER.readValue(json, DBUserBean.class)))
                .map(DBUser::new)
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBUser {}] Not found", userId.asString());
                    }
                })
                .defaultIfEmpty(new DBUser(userId))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[DBUser {}] Request", userId.asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(this.getName()).inc();
                });

        return this.usersCache.getOrCache(userId, getDBUser);
    }

    public void invalidateCache(Snowflake userId) {
        LOGGER.trace("{User ID: {}} Cache invalidated", userId.asString());
        this.usersCache.remove(userId);
    }

}
