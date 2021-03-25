package com.shadorc.shadbot.cache;

import discord4j.common.util.Snowflake;

public class CacheManager {

    private static CacheManager instance;

    static {
        CacheManager.instance = new CacheManager();
    }

    private final MapCache<Snowflake, Snowflake> guildOwnersCache;

    private CacheManager() {
        this.guildOwnersCache = new MapCache<>();
    }

    public MapCache<Snowflake, Snowflake> getGuildOwnersCache() {
        return this.guildOwnersCache;
    }

    public static CacheManager getInstance() {
        return CacheManager.instance;
    }

}
