package com.shadorc.shadbot.db.premium;

import com.rethinkdb.gen.ast.ReqlExpr;
import com.rethinkdb.net.Cursor;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.db.premium.bean.RelicBean;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.object.util.Snowflake;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PremiumManager extends DatabaseTable {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.database.premium");

    private static PremiumManager instance;

    static {
        PremiumManager.instance = new PremiumManager();
    }

    private PremiumManager() {
        super("premium");
    }

    public ReqlExpr requestRelic(String id) {
        return this.getTable()
                .filter(this.getDatabase().hashMap("id", id));
    }

    public Optional<Relic> getRelicById(String relicId) {
        LOGGER.debug("Requesting Relic with ID {}.", relicId);

        try (final Cursor<String> cursor = this.requestRelic(relicId).map(ReqlExpr::toJson).run(this.getConnection())) {
            if (cursor.hasNext()) {
                LOGGER.debug("Relic with ID {} found.", relicId);
                return Optional.of(new Relic(Utils.MAPPER.readValue(cursor.next(), RelicBean.class)));
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while requesting Relic with ID %s.", relicId));
        }
        LOGGER.debug("Relic with ID {} not found.", relicId);
        return Optional.empty();
    }

    public List<Relic> getRelicsByUser(Snowflake userId) {
        LOGGER.debug("Requesting Relics for user ID {}.", userId.asLong());

        final List<Relic> relics = new ArrayList<>();
        final ReqlExpr request = this.getTable()
                .filter(this.getDatabase().hashMap("user_id", userId.asLong()))
                .map(ReqlExpr::toJson);

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            while (cursor.hasNext()) {
                relics.add(new Relic(Utils.MAPPER.readValue(cursor.next(), RelicBean.class)));
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while requesting Relic for user ID %d.", userId.asLong()));
        }

        return relics;
    }

    public List<Relic> getRelicsByGuild(Snowflake guildId) {
        LOGGER.debug("Requesting Relics for guild ID {}.", guildId.asLong());

        final List<Relic> relics = new ArrayList<>();
        final ReqlExpr request = this.getTable()
                .filter(this.getDatabase().hashMap("guild_id", guildId.asLong()))
                .map(ReqlExpr::toJson);

        try (final Cursor<String> cursor = request.run(this.getConnection())) {
            while (cursor.hasNext()) {
                relics.add(new Relic(Utils.MAPPER.readValue(cursor.next(), RelicBean.class)));
            }
        } catch (final Exception err) {
            LogUtils.error(Shadbot.getClient(), err,
                    String.format("An error occurred while requesting Relic for guild ID %d.", guildId.asLong()));
        }

        return relics;
    }

    public Relic generateRelic(RelicType type) {
        final Relic relic = new Relic(UUID.randomUUID(), type, Duration.ofDays(Config.DEFAULT_RELIC_DURATION));
        relic.insert();
        return relic;
    }

    public boolean isUserPremium(Snowflake userId) {
        return !this.getRelicsByUser(userId).isEmpty();
    }

    public boolean isGuildPremium(Snowflake guildId) {
        return !this.getRelicsByGuild(guildId).isEmpty();
    }

    public static PremiumManager getInstance() {
        return PremiumManager.instance;
    }

}
