package com.shadorc.shadbot.db.guild;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

import java.io.IOException;

import static com.shadorc.shadbot.db.guild.GuildManager.LOGGER;

public class DBMember implements DatabaseEntity {

    private long guildId;
    private long id;
    private int coins;

    public DBMember(Snowflake guildId, Snowflake id) {
        this.guildId = guildId.asLong();
        this.id = id.asLong();
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getId() {
        return Snowflake.of(this.id);
    }

    public int getCoins() {
        return this.coins;
    }

    public int addCoins(long gains) {
        final int coins = (int) NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        try {
            LOGGER.debug("[DBMember {} / {}] Updating coins {}", this.getId().asLong(), this.guildId, coins);
            final GuildManager gm = GuildManager.getInstance();
            gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.guildId)
                            .with("members", gm.getDatabase().hashMap("id", this.getId().asLong())
                                    .with("coins", coins)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred while updating coins.",
                            this.getId().asLong(), this.guildId));
        }

        return coins;
    }

    public void resetCoins() {
        try {
            LOGGER.debug("[DBMember {} / {}] Resetting coins.", this.getId().asLong(), this.guildId);
            final GuildManager gm = GuildManager.getInstance();
            gm.requestMember(this.getGuildId(), this.getId())
                    .replace(member -> member.without("coins"))
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred while resetting coins.",
                            this.getId().asLong(), this.guildId));
        }
    }

    @Override
    public void readValue(String content) throws IOException {
        final DBMemberBean bean = Utils.MAPPER.readValue(content, DBMemberBean.class);

        this.id = bean.getId();
        this.coins = bean.getCoins();
    }

    @Override
    public void insert() {
        try {
            LOGGER.debug("[DBMember {} / {}] Inserting...", this.getId().asLong(), this.guildId);
            final GuildManager gm = GuildManager.getInstance();
            gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.guildId)
                            .with("members", gm.getDatabase().hashMap("id", this.getId().asLong())))
                    .optArg("conflict", "update")
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during insertion.", this.getId().asLong(), this.guildId));
        }
        LOGGER.debug("[DBMember {} / {}] Inserted.", this.getId().asLong(), this.guildId);
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[DBMember {} / {}] Deleting...", this.getId().asLong(), this.guildId);
            GuildManager.getInstance()
                    .requestMember(this.getGuildId(), this.getId())
                    .delete()
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during deletion.",
                            this.getId().asLong(), this.guildId));
        }
        LOGGER.debug("[DBMember {} / {}] Deleted.", this.getId().asLong(), this.guildId);
    }

    @Override
    public String toString() {
        return "DBMember{" +
                "guildId=" + this.guildId +
                ", id=" + this.id +
                ", coins=" + this.coins +
                '}';
    }
}
