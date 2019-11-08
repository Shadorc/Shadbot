package com.shadorc.shadbot.db.lottery.entity;

import com.rethinkdb.model.MapObject;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.lottery.LotteryManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.util.Snowflake;

import static com.shadorc.shadbot.db.lottery.LotteryManager.LOGGER;

public class LotteryGambler implements DatabaseEntity {

    private final long guildId;
    private final long userId;
    private final int number;

    public LotteryGambler(LotteryGamblerBean bean) {
        this.guildId = bean.getGuildId();
        this.userId = bean.getUserId();
        this.number = bean.getNumber();
    }

    public LotteryGambler(Snowflake guildId, Snowflake userId, int number) {
        this.guildId = guildId.asLong();
        this.userId = userId.asLong();
        this.number = number;
    }

    public Snowflake getGuildId() {
        return Snowflake.of(this.guildId);
    }

    public Snowflake getUserId() {
        return Snowflake.of(this.userId);
    }

    public int getNumber() {
        return this.number;
    }

    @Override
    public void insert() {
        LOGGER.debug("[LotteryGambler {} / {}] Inserting...", this.getUserId().asLong(), this.getGuildId().asLong());
        try {
            final LotteryManager lm = LotteryManager.getInstance();

            final MapObject gambler = lm.getDatabase()
                    .hashMap("guild_id", this.guildId)
                    .with("user_id", this.userId)
                    .with("number", this.number);

            final String response = lm.getTable()
                    .update(row -> lm.getDatabase().hashMap("gamblers", row.getField("gamblers")
                            .default_(lm.getDatabase().array())
                            .append(gambler)))
                    .run(lm.getConnection())
                    .toString();

            LOGGER.debug("[LotteryGambler {} / {}] {}", this.getUserId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[LotteryGambler %d / %d] An error occurred during insertion.", this.getUserId().asLong(), this.getGuildId().asLong()));
        }
    }

    @Override
    public void delete() {
        LOGGER.debug("[LotteryGambler {} / {}] Deleting...", this.getUserId().asLong(), this.getGuildId().asLong());
        try {
            final LotteryManager lm = LotteryManager.getInstance();
            final String response = lm.requestGambler(this.getGuildId(), this.getUserId())
                    .delete()
                    .run(lm.getConnection())
                    .toString();

            LOGGER.debug("[LotteryGambler {} / {}] {}", this.getUserId().asLong(), this.getGuildId().asLong(), response);

        } catch (final RuntimeException err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[LotteryGambler %d / %d] An error occurred during deletion.", this.getUserId().asLong(), this.getGuildId().asLong()));
        }
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "guildId=" + this.guildId +
                ", userId=" + this.userId +
                ", number=" + this.number +
                '}';
    }

}
