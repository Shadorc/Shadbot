package com.shadorc.shadbot.db.users;

import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoDatabase;
import com.shadorc.shadbot.db.DatabaseCollection;
import com.shadorc.shadbot.db.users.bean.DBUserBean;
import com.shadorc.shadbot.db.users.entity.DBUser;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import static com.shadorc.shadbot.db.DatabaseManager.DB_REQUEST_COUNTER;

public class UsersCollection extends DatabaseCollection {

    public static final Logger LOGGER = LogUtils.getLogger(UsersCollection.class, LogUtils.Category.DATABASE);
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
                        new DBUser(NetUtils.MAPPER.readValue(document.toJson(JSON_WRITER_SETTINGS), DBUserBean.class))))
                .doOnSuccess(consumer -> {
                    if (consumer == null) {
                        LOGGER.debug("[DBUser {}] Not found", id.asLong());
                    }
                })
                .defaultIfEmpty(new DBUser(id))
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels(UsersCollection.NAME).inc());
    }

}
