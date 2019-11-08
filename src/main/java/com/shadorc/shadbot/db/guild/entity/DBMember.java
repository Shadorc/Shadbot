package com.shadorc.shadbot.db.guild.entity;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.db.guild.bean.DBMemberBean;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.util.Snowflake;

import static com.shadorc.shadbot.db.guild.GuildManager.LOGGER;

public class DBMember implements DatabaseEntity {

    private final long guildId;
    private final long id;
    private final int coins;

    public DBMember(Snowflake guildId, DBMemberBean bean) {
        this.guildId = guildId.asLong();
        this.id = bean.getId();
        this.coins = bean.getCoins();
    }

    public DBMember(Snowflake guildId, Snowflake id) {
        this.guildId = guildId.asLong();
        this.id = id.asLong();
        this.coins = 0;
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

        LOGGER.debug("[DBMember {} / {}] Updating coins {}", this.getId().asLong(), this.getGuildId().asLong(), coins);
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.getGuildId().asLong())
                            .with("members", gm.getDatabase().hashMap("id", this.getId().asLong())
                                    .with("coins", coins)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection())
                    .toString();

            LOGGER.debug("[DBMember {} / {}] {}", this.getId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred while updating coins.",
                            this.getId().asLong(), this.getGuildId().asLong()));
        }

        return coins;
    }

    public void resetCoins() {
        LOGGER.debug("[DBMember {} / {}] Resetting coins.", this.getId().asLong(), this.getGuildId().asLong());
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.requestMember(this.getGuildId(), this.getId())
                    .replace(member -> member.without("coins"))
                    .run(gm.getConnection());

            LOGGER.debug("[DBMember {} / {}] {}.", this.getId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred while resetting coins.",
                            this.getId().asLong(), this.getGuildId().asLong()));
        }
    }

    @Override
    public void insert() {
        LOGGER.debug("[DBMember {} / {}] Inserting...", this.getId().asLong(), this.getGuildId().asLong());
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.getGuildId().asLong())
                            .with("members", gm.getDatabase().hashMap("id", this.getId().asLong())))
                    .optArg("conflict", "update")
                    .run(gm.getConnection())
                    .toString();

            LOGGER.debug("[DBMember {} / {}] {}", this.getId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during insertion.",
                            this.getId().asLong(), this.getGuildId().asLong()));
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[DBMember {} / {}] Deleting...", this.getId().asLong(), this.getGuildId().asLong());
        try {
            final GuildManager gm = GuildManager.getInstance();
            final String response = gm.requestMember(this.getGuildId(), this.getId())
                    .delete()
                    .run(gm.getConnection());

            LOGGER.debug("[DBMember {} / {}] {}", this.getId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during deletion.",
                            this.getId().asLong(), this.getGuildId().asLong()));
        }
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
