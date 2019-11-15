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
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.*;

public final class LotteryManager extends DatabaseTable {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.lottery");

    private static LotteryManager instance;

    static {
        LotteryManager.instance = new LotteryManager();
    }

    private LotteryManager() {
        super("lottery");
    }

    public ReqlExpr requestGambler(Snowflake guildId, Snowflake userId) {
        return this.getTable()
                .filter(this.getDatabase().hashMap("guild_id", guildId.asLong())
                        .with("user_id", userId.asLong()));
    }

    public ReqlExpr requestHistoric() {
        return this.getTable()
                .getField("historic");
    }

    public Optional<LotteryHistoric> getHistoric() {
        LOGGER.debug("Requesting Lottery historic.");

        final ReqlExpr request = this.requestHistoric().map(ReqlExpr::toJson);

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                LOGGER.debug("Lottery historic found.");
                return Optional.of(new LotteryHistoric(Utils.MAPPER.readValue(cursor.next(), LotteryHistoricBean.class)));
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting lottery historic.");
        }
        LOGGER.debug("Lottery historic not found.");
        return Optional.empty();
    }

    public long getJackpot() {
        LOGGER.debug("Requesting Lottery jackpot.");

        final ReqlExpr request = this.getTable()
                .getField("jackpot");

        try (final Cursor<Long> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                LOGGER.debug("Lottery jackpot found.");
                return cursor.next();
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting lottery jackpot.");
        }
        LOGGER.debug("Lottery jackpot not found.");
        return 0;
    }

    public void addToJackpot(long coins) {
        final int value = (int) Math.min(Math.ceil(coins / 100.0f), Config.MAX_COINS);

        LOGGER.debug("Adding {} to Lottery jackpot.", FormatUtils.coins(value));
        try {
            final String response = this.getTable()
                    .insert(this.getDatabase().hashMap("id", "jackpot").with("jackpot", value))
                    .optArg("conflict", (id, oldDoc, newDoc) -> newDoc.merge(
                            this.getDatabase().hashMap("jackpot", oldDoc.g("jackpot").add(value))))
                    .run(this.getConnection())
                    .toString();

            LOGGER.debug("Lottery jackpot addition response: {}", response);
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while adding to jackpot.");
        }
    }

    public void resetJackpot() {
        LOGGER.debug("Resetting Lottery jackpot.");

        try {
            final String response = this.getTable()
                    .replace(this.getDatabase().hashMap("id", "jackpot").with("jackpot", 0))
                    .run(this.getConnection())
                    .toString();

            LOGGER.debug("Lottery jackpot reset response: {}", response);
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while resetting jackpot.");
        }
    }

    public List<LotteryGambler> getGamblers() {
        LOGGER.debug("Requesting Lottery gamblers.");

        final ReqlExpr request = this.getTable()
                .filter(row -> row.hasFields("guild_id", "user_id", "number"))
                .map(ReqlExpr::toJson);

        final List<LotteryGambler> gamblers = new ArrayList<>();
        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            if (cursor.hasNext()) {
                Arrays.stream(Utils.MAPPER.readValue(cursor.next(), LotteryGamblerBean[].class))
                        .map(LotteryGambler::new)
                        .forEach(gamblers::add);
            }

        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while getting gamblers.");
        }

        return Collections.unmodifiableList(gamblers);
    }

    public void resetGamblers() {
        LOGGER.debug("Resetting Lottery gamblers.");

        try {
            this.getTable()
                    .filter(row -> row.hasFields("guild_id", "user_id", "number"))
                    .delete()
                    .run(this.getConnection());
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err, "An error occurred while resetting gamblers.");
        }
    }

    public static LotteryManager getInstance() {
        return LotteryManager.instance;
    }

}
