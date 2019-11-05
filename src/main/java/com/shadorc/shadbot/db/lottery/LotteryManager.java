package com.shadorc.shadbot.db.lottery;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.db.lottery.bean.LotteryGamblerBean;
import com.shadorc.shadbot.db.lottery.bean.LotteryHistoricBean;
import com.shadorc.shadbot.db.lottery.entity.LotteryGambler;
import com.shadorc.shadbot.db.lottery.entity.LotteryHistoric;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class LotteryManager extends DatabaseTable {

    private static LotteryManager instance;

    static {
        LotteryManager.instance = new LotteryManager();
    }

    private LotteryManager() {
        super("lottery");
    }

    public Optional<LotteryHistoric> getHistoric() {
        final ReqlExpr request = this.getTable()
                .filter(lottery -> lottery.hasFields("historic"))
                .map(ReqlExpr::toJson);

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                return Optional.of(new LotteryHistoric(Utils.MAPPER.readValue(cursor.next(), LotteryHistoricBean.class)));
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting lottery historic.");
        }
        return Optional.empty();
    }

    public long getJackpot() {
        final ReqlExpr request = this.getTable()
                .filter(lottery -> lottery.hasFields("jackpot"));

        try (final Cursor<Long> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                return cursor.next();
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting lottery jackpot.");
        }
        return 0;
    }

    public void addToJackpot(long coins) {
        try {
            this.getTable().get("jackpot")
                    .update(jackpot -> jackpot.add((int) Math.ceil(coins / 100.0f))
                            .default_(0)
                            .max(Config.MAX_COINS))
                    .run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while adding to jackpot.");
        }
    }

    public void resetJackpot() {
        try {
            this.getTable().update(this.getDatabase().hashMap("jackpot", 0)).run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while resetting jackpot.");
        }
    }

    public List<LotteryGambler> getGamblers() {
        final ReqlExpr request = this.getTable()
                .filter(lottery -> lottery.hasFields("gamblers"))
                .getField("gamblers")
                .map(ReqlExpr::toJson);

        final List<LotteryGambler> gamblers = new ArrayList<>();
        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            while (cursor.hasNext()) {
                gamblers.add(new LotteryGambler(Utils.MAPPER.readValue(cursor.next(), LotteryGamblerBean.class)));
            }

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting gamblers.");
        }

        return Collections.unmodifiableList(gamblers);
    }

    public void addGambler(Snowflake guildId, Snowflake userId, int number) {
        try {
            this.getTable()
                    .get("gamblers")
                    .update(gamblers -> gamblers.add(this.getDatabase().hashMap("guild_id", guildId.asLong())
                            .with("user_id", userId.asLong())
                            .with("number", number)))
                    .run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while adding gambler.");
        }
    }

    public void resetGamblers() {
        try {
            this.getTable().update(this.getDatabase().hashMap("gamblers", this.getDatabase().array())).run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while resetting jackpot.");
        }
    }

    public static LotteryManager getInstance() {
        return LotteryManager.instance;
    }

}
