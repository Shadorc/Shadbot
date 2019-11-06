package com.shadorc.shadbot.db.lottery.entity;

import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.db.DatabaseEntity;
import com.shadorc.shadbot.db.lottery.LotteryManager;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.object.util.Snowflake;

import static com.shadorc.shadbot.db.premium.PremiumManager.LOGGER;

public class LotteryGambler implements DatabaseEntity {

    private long guildId;
    private long userId;
    private int number;

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
        return number;
    }

    @Override
    public void insert() {
        try {
            LOGGER.debug("[LotteryGambler {} / {}] Inserting...", this.getUserId().asLong(), this.getGuildId().asLong());
            final LotteryManager lm = LotteryManager.getInstance();
            lm.getTable()
                    .insert(lm.getDatabase()
                            .hashMap("guild_id", this.guildId)
                            .with("user_id", this.userId)
                            .with("number", this.number))
                    .run(LotteryManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[LotteryGambler %d / %d] An error occurred during insertion.", this.getUserId().asLong(), this.getGuildId().asLong()));
        }
        LOGGER.debug("[LotteryGambler {} / {}] Inserted.", this.getUserId().asLong(), this.getGuildId().asLong());
    }

    @Override
    public void delete() {
        try {
            LOGGER.debug("[LotteryGambler {} / {}] Deleting...", this.getUserId().asLong(), this.getGuildId().asLong());
            LotteryManager.getInstance()
                    .requestGambler(this.getGuildId(), this.getUserId())
                    .delete()
                    .run(LotteryManager.getInstance().getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("[LotteryGambler %d / %d] An error occurred during deletion.", this.getUserId().asLong(), this.getGuildId().asLong()));
        }
        LOGGER.debug("[LotteryGambler {} / {}] Deleted.", this.getUserId().asLong(), this.getGuildId().asLong());
    }

    @Override
    public String toString() {
        return "LotteryGambler{" +
                "guildId=" + guildId +
                ", userId=" + userId +
                ", number=" + number +
                '}';
    }

}
