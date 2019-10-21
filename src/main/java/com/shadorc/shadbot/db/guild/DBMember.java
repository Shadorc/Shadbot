package com.shadorc.shadbot.db.guild;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NumberUtils;
import discord4j.core.object.util.Snowflake;

import static com.shadorc.shadbot.db.guild.GuildManager.LOGGER;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class DBMember extends DatabaseEntity {

    private final long guildId;
    @JsonProperty("id")
    private final long memberId;
    @JsonProperty("coins")
    private final int coins;

    protected DBMember(Snowflake guildId, Snowflake id) {
        this.guildId = guildId.asLong();
        this.memberId = id.asLong();
        this.coins = 0;
    }

    protected DBMember() {
        this(Snowflake.of(0L), Snowflake.of(0L));
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getId() {
        return Snowflake.of(this.memberId);
    }

    public int getCoins() {
        return this.coins;
    }

    public int addCoins(long gains) {
        final int coins = (int) NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS);

        try {
            LOGGER.debug("[DBMember {} / {}] Updating coins {}", this.memberId, this.guildId, coins);
            final GuildManager gm = GuildManager.getInstance();
            gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.guildId)
                            .with("members", gm.getDatabase().hashMap("id", this.memberId)
                                    .with("coins", coins)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(/*Shadbot.getClient(), */err,
                    String.format("[DBMember %d / %d] An error occurred while updating coins.",
                            this.memberId, this.guildId));
        }

        return coins;
    }

    public void resetCoins() {
        try {
            LOGGER.debug("[DBMember {} / {}] Resetting coins.", this.memberId, this.guildId);
            final GuildManager gm = GuildManager.getInstance();
            gm.requestMember(this.getGuildId(), this.getId())
                    .replace(member -> member.without("coins"))
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred while resetting coins.",
                            this.memberId, this.guildId));
        }
    }

    @Override
    public void insert() {
        try {
            LOGGER.debug("[DBMember {} / {}] Inserting...", this.memberId, this.guildId);
            final GuildManager gm = GuildManager.getInstance();
            gm.getTable()
                    .insert(gm.getDatabase().hashMap("id", this.guildId)
                            .with("members", gm.getDatabase().hashMap("id", this.memberId)))
                    .optArg("conflict", "update")
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during insertion.", this.memberId, this.guildId));
        }
        LOGGER.debug("[DBMember {} / {}] Inserted.", this.memberId, this.guildId);
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[DBMember {} / {}] Deleting...", this.memberId, this.guildId);
            GuildManager.getInstance()
                    .requestMember(this.getGuildId(), this.getId())
                    .delete()
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[DBMember %d / %d] An error occurred during deletion.",
                            this.memberId, this.guildId));
        }
        LOGGER.debug("[DBMember {} / {}] Deleted.", this.memberId, this.guildId);
    }

    @Override
    public String toString() {
        return String.format("DBMember [guildId=%s, memberId=%s, coins=%s]",
                this.guildId, this.memberId, this.coins);
    }
}
