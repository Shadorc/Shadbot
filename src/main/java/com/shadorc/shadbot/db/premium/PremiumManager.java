package com.shadorc.shadbot.db.premium;

import com.shadorc.shadbot.db.DatabaseTable;
import com.shadorc.shadbot.db.premium.entity.GuildRelic;
import com.shadorc.shadbot.db.premium.entity.Relic;
import com.shadorc.shadbot.db.premium.entity.UserRelic;
import discord4j.core.object.util.Snowflake;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PremiumManager extends DatabaseTable {

    private static PremiumManager instance;

    static {
        PremiumManager.instance = new PremiumManager();
    }

    private PremiumManager() {
        super("premium");
    }

    public Relic getRelicById(String relicId) {
        // TODO
        return null;
    }

    public List<Relic> getRelicsByUser(Snowflake userId) {
        // TODO
        return null;
    }

    public UserRelic generateUserRelic(Snowflake userId) {
        final UserRelic relic = new UserRelic(userId, UUID.randomUUID().toString(), TimeUnit.DAYS.toMillis(180));
        // TODO: save
        return relic;
    }

    public GuildRelic generateGuildRelic(Snowflake guildId) {
        final GuildRelic relic = new GuildRelic(guildId, UUID.randomUUID().toString(), TimeUnit.DAYS.toMillis(180));
        // TODO: save
        return relic;
    }

    public boolean isUserPremium(Snowflake userId) {
        // TODO
        return false;
    }

    public boolean isGuildPremium(Snowflake guildId) {
        // TODO
        return false;
    }

    public static PremiumManager getInstance() {
        return PremiumManager.instance;
    }

}
