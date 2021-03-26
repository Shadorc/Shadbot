package com.shadorc.shadbot.db.lottery.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.Objects;

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
        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .updateOne(Filters.eq("_id", "gamblers"),
                        Updates.push("gamblers", this.toDocument()),
                        new UpdateOptions().upsert(true)))
                .doOnNext(result -> LOGGER.trace("[LotteryGambler {}/{}] Insertion result: {}",
                        this.getUserId().asString(), this.getGuildId().asString(), result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[LotteryGambler {}/{}] Insertion",
                            this.getUserId().asString(), this.getGuildId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc();
                })
                .doOnTerminate(DatabaseManager.getLottery()::invalidateGamblersCache)
                .then();
    }

    @Override
    public Mono<Void> delete() {
        return Mono.from(DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.and(
                        Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.guild_id", this.getGuildId().asString()),
                        Filters.eq("gamblers.user_id", this.getUserId().asString()))))
                .doOnNext(result -> LOGGER.trace("[LotteryGambler {}/{}] Deletion result: {}",
                        this.getUserId().asString(), this.getGuildId().asString(), result))
                .doOnSubscribe(__ -> {
                    LOGGER.debug("[LotteryGambler {}/{}] Deletion",
                            this.getUserId().asString(), this.getGuildId().asString());
                    Telemetry.DB_REQUEST_COUNTER.labels(DatabaseManager.getLottery().getName()).inc();
                })
                .doOnTerminate(DatabaseManager.getLottery()::invalidateGamblersCache)
                .then();
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "bean=" + this.getBean() +
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
        final LotteryGambler lotteryGambler = (LotteryGambler) obj;
        return Objects.equals(this.getBean().getGuildId(), lotteryGambler.getBean().getGuildId())
                && Objects.equals(this.getBean().getUserId(), lotteryGambler.getBean().getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getBean().getGuildId(), this.getBean().getUserId());
    }
}
