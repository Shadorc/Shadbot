package com.shadorc.shadbot.db.users;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.users.bean.DBUserBean;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.NetUtil;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

public class UsersCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtil.getLogger(UsersCollection.class, LogUtil.Category.DATABASE);
    public static final String NAME = "users";

    public UsersCollection(MongoDatabase database) {
        super(database.getCollection(UsersCollection.NAME));
    }

    public Mono<DBUser> getDBUser(Snowflake id) {
        LOGGER.debug("[DBUser {}] Request", id.asLong());

        final Publisher<Document> request = this.getCollection()
                .find(Filters.eq("_id", id.asString()))
                .first();

        return Mono.from(request)
                .flatMap(document -> Mono.fromCallable(() ->
                        new DBUser(NetUtil.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBUserBean.class))))
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBUser {}] Not found", id.asLong());
                    }
                })
                .defaultIfEmpty(new DBUser(id))
                .doOnTerminate(() -> Telemetry.DB_REQUEST_COUNTER.labels(UsersCollection.NAME).inc());
    }

}
