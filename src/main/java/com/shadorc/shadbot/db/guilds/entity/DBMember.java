package com.shadorc.shadbot.db.guilds.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.bean.DBMemberBean;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import org.bson.Document;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBMember implements DatabaseEntity {

    private final String guildId;
    private final DBMemberBean bean;

    public DBMember(Snowflake guildId, DBMemberBean bean) {
        this.guildId = guildId.asString();
        this.bean = bean;
    }

    public DBMember(Snowflake guildId, Snowflake id) {
        this.guildId = guildId.asString();
        this.bean = new DBMemberBean(id.asString(), 0);
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getId() {
        return Snowflake.of(this.bean.getId());
    }

    public int getCoins() {
        return this.bean.getCoins();
    }

    public void addCoins(long gains) {
        final int coins = (int) NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        LOGGER.debug("[DBMember {} / {}] Updating coins {}", this.getId().asLong(), this.getGuildId().asLong(), coins);

        final long modifiedCount = DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("members._id", this.getId().asString()),
                        Updates.set("members.$.coins", coins))
                .getModifiedCount();

        if (modifiedCount == 0) {
            try {
                DatabaseManager.getGuilds()
                        .getCollection()
                        .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                                Updates.push("members", this.toDocument().append("coins", coins)),
                                new UpdateOptions().upsert(true));
            } catch (final JsonProcessingException err) {
                throw new RuntimeException(err);
            }
        }

    }

    // Note: If one day, a member contains more data than just coins, this method will need to be updated
    public void resetCoins() {
        LOGGER.debug("[DBMember {} / {}] Resetting coins.", this.getId().asLong(), this.getGuildId().asLong());
        this.delete();
    }

    @Override
    public void insert() {
        LOGGER.debug("[DBMember {} / {}] Insertion", this.getId().asLong(), this.getGuildId().asLong());

        try {
            DatabaseManager.getGuilds()
                    .getCollection()
                    .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                            Updates.push("members", this.toDocument()),
                            new UpdateOptions().upsert(true));
        } catch (final JsonProcessingException err) {
            throw new RuntimeException(err);
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[DBMember {} / {}] Deletion", this.getId().asLong(), this.getGuildId().asLong());

        DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.pull("members", Filters.eq("_id", this.getId().asString())));
    }

    @Override
    public Document toDocument() throws JsonProcessingException {
        return Document.parse(Utils.MAPPER.writeValueAsString(this.bean));
    }

    @Override
    public String toString() {
        return "DBMember{" +
                "guildId='" + this.guildId + '\'' +
                ", bean=" + this.bean +
                '}';
    }
}
