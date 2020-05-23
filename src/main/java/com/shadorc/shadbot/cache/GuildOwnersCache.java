package com.shadorc.shadbot.cache;

import discord4j.common.util.Snowflake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuildOwnersCache {

    private static final Map<Long, Long> MAP = new ConcurrentHashMap<>();

    public static void put(Snowflake guildId, Snowflake ownerId) {
        MAP.put(guildId.asLong(), ownerId.asLong());
    }

    public static void remove(Snowflake guildId) {
        MAP.remove(guildId.asLong());
    }

    public static Snowflake get(Snowflake guildId) {
        return Snowflake.of(MAP.get(guildId.asLong()));
    }

    public static long count() {
        return MAP.size();
    }

}
