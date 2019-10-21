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

import java.util.concurrent.atomic.AtomicInteger;

@JsonAutoDetect(getterVisibility = Visibility.NONE)
public class DBMember extends DatabaseEntity {

    private final long guildId;
    @JsonProperty("id")
    private final long memberId;
    @JsonProperty("coins")
    private final AtomicInteger coins;

    protected DBMember(Snowflake guildId, Snowflake id) {
        this.guildId = guildId.asLong();
        this.memberId = id.asLong();
        this.coins = new AtomicInteger();
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
        return this.coins.get();
    }

    // TODO: Optimize
    public void addCoins(long gains) {
        this.coins.set((int) NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS));

        try {
            final GuildManager gm = GuildManager.getInstance();
            final boolean memberExists = gm.requestMember(this.getGuildId(), this.getId())
                    .count().eq(1).run(gm.getConnection());
            if (!memberExists) {
                this.insert();
            }

            gm.requestMember(this.getGuildId(), this.getId())
                    .update(gm.getDatabase().hashMap("coins", this.getCoins()))
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while adding coins DBMember with ID %d.", this.memberId));
        }
    }

    // TODO: Optimize
    public void resetCoins() {
        this.coins.set(0);

        try {
            final GuildManager gm = GuildManager.getInstance();
            gm.requestMember(this.getGuildId(), this.getId())
                    .replace(member -> member.without("coins"))
                    .run(gm.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while resetting coins DBMember with ID %d.", this.memberId));
        }
    }

    @Override
    public void insert() {

    }

    @Override
    public void delete() {
        try {
            GuildManager.getInstance()
                    .requestMember(this.getGuildId(), this.getId())
                    .delete()
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while deleting DBMember with ID %d and Guild ID %d.",
                            this.memberId, this.guildId));
        }
    }

    @Override
    public String toString() {
        return String.format("DBMember [guildId=%s, memberId=%s, coins=%s]",
                this.guildId, this.memberId, this.coins);
    }
}
