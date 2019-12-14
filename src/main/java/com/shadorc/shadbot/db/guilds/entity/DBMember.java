package com.shadorc.shadbot.db.guilds.entity;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.SerializableEntity;
import com.shadorc.shadbot.db.guilds.bean.DBMemberBean;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.util.Snowflake;

import static com.shadorc.shadbot.db.guilds.GuildsCollection.LOGGER;

public class DBMember extends SerializableEntity<DBMemberBean> implements DatabaseEntity {

    private final String guildId;

    public DBMember(Snowflake guildId, DBMemberBean bean) {
        super(bean);
        this.guildId = guildId.asString();
    }

    public DBMember(Snowflake guildId, Snowflake id) {
        super(new DBMemberBean(id.asString(), 0));
        this.guildId = guildId.asString();
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getId() {
        return Snowflake.of(this.getBean().getId());
    }

    public long getCoins() {
        return this.getBean().getCoins();
    }

    public void addCoins(long gains) {
        final long coins = NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        // The user has already exceeded the maximum number of coins, no need to update him
        if (coins == Config.MAX_COINS) {
            return;
        }

        LOGGER.debug("[DBMember {} / {}] Updating coins {}", this.getId().asLong(), this.getGuildId().asLong(), coins);

        final long matchedCount = DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(
                        Filters.eq("members._id", this.getId().asString()),
                        Updates.set("members.$.coins", coins))
                .getMatchedCount();

        // Member was not find, insert it
        if (matchedCount == 0) {
            DatabaseManager.getGuilds()
                    .getCollection()
                    .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                            Updates.push("members", this.toDocument().append("coins", coins)),
                            new UpdateOptions().upsert(true));
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

        DatabaseManager.getGuilds()
                .getCollection()
                .updateOne(Filters.eq("_id", this.getGuildId().asString()),
                        Updates.push("members", this.toDocument()),
                        new UpdateOptions().upsert(true));
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
    public String toString() {
        return "DBMember{" +
                "guildId='" + this.guildId + '\'' +
                ", bean=" + this.getBean() +
                '}';
    }
}
