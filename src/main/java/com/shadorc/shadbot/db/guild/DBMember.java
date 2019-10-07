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

    public void addCoins(long gains) {
        this.coins.set((int) NumberUtils.truncateBetween(this.getCoins() + gains, 0, Config.MAX_COINS));
        this.update("coins", this.getCoins());
    }

    public void resetCoins() {
        this.coins.set(0);
        this.update("coins", this.getCoins());
    }

    @Override
    protected void update(String value, Object field) {
        try {
            // TODO: What if the member is not present ?
            GuildManager.getInstance()
                    .requestMember(this.getGuildId(), this.getId())
                    .forEach(member -> member.update(GuildManager.getInstance().getDatabase().hashMap(value, field)))
                    .run(GuildManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while updating DBMember with ID %d and Guild ID %d.",
                            this.memberId, this.guildId));
        }
    }

    // TODO: This method needs two accesses to the database, needs refactoring
    @Override
    public void delete() {
        try {
            // TODO: What if the member is not present ?
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
        return String.format("DBMember [guildId=%s, memberId=%s, coins=%s]", this.guildId, this.memberId, this.coins);
    }
}
