package com.shadorc.shadbot.db.lottery.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.db.DatabaseManager.DB_REQUEST_COUNTER;
import static com.shadorc.shadbot.db.lottery.LotteryCollection.LOGGER;

public class LotteryGambler extends SerializableEntity<LotteryGamblerBean> implements DatabaseEntity {

    public LotteryGambler(LotteryGamblerBean bean) {
        super(bean);
    }

    public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
        super(new LotteryGamblerBean(guildId.asString(), userId.asString(), number));
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.getBean().getGuildId());
    }

    public Snowflake getUserId() {
        return Snowflake.of(this.getBean().getUserId());
    }

    public int getNumber() {
        return this.getBean().getNumber();
    }

    @Override
    public Mono<Void> insert() {
        LOGGER.debug("[LotteryGambler {} / {}] Insertion", this.getUserId().asLong(), this.getGuildId().asLong());

        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .updateOne(Filters.eq("_id", "gamblers"),
                        Updates.push("gamblers", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[LotteryGambler {} / {}] Insertion result: {}",
                        this.getUserId().asLong(), this.getGuildId().asLong(), result))
                .then()
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels("lottery").inc());
    }

    @Override
    public Mono<Void> delete() {
        LOGGER.debug("[LotteryGambler {} / {}] Deletion", this.getUserId().asLong(), this.getGuildId().asLong());

        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.and(
                        Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.guild_id", this.getGuildId().asString()),
                        Filters.eq("gamblers.user_id", this.getUserId().asString()))))
                .doOnNext(result -> LOGGER.trace("[LotteryGambler {} / {}] Deletion result: {}",
                        this.getUserId().asLong(), this.getGuildId().asLong(), result))
                .then()
                .doOnTerminate(() -> DB_REQUEST_COUNTER.labels("lottery").inc());
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "bean=" + this.getBean() +
                '}';
    }
}
