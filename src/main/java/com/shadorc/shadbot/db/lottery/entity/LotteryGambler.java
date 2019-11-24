package com.shadorc.shadbot.db.lottery.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;

import static com.shadorc.shadbot.db.lottery.LotteryCollection.LOGGER;

public class LotteryGambler implements DatabaseEntity {

    private final LotteryGamblerBean bean;

    public LotteryGambler(LotteryGamblerBean bean) {
        this.bean = bean;
    }

    public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
        this.bean = new LotteryGamblerBean(guildId.asString(), userId.asString(), number);
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.bean.getGuildId());
    }

    public Snowflake getUserId() {
        return Snowflake.of(this.bean.getUserId());
    }

    public int getNumber() {
        return this.bean.getNumber();
    }

    @Override
    public void insert() {
        LOGGER.debug("[LotteryGambler {} / {}] Insertion", this.getUserId().asLong(), this.getGuildId().asLong());

        try {
            DatabaseManager.getLottery()
                    .getCollection()
                    .updateOne(Filters.eq("_id", "gamblers"),
                            Updates.push("gamblers", this.toDocument()),
                            new UpdateOptions().upsert(true));
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[LotteryGambler {} / {}] Deletion", this.getUserId().asLong(), this.getGuildId().asLong());

        DatabaseManager.getLottery()
                .getCollection()
                .deleteOne(Filters.and(Filters.eq("_id", "gamblers"),
                        Filters.eq("gamblers.guild_id", this.getGuildId().asString()),
                        Filters.eq("gamblers.user_id", this.getUserId().asString())));
    }

    @Override
    public Document toDocument() throws JsonProcessingException {
        return Document.parse(Utils.MAPPER.writeValueAsString(this.bean));
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "bean=" + this.bean +
                '}';
    }
}
