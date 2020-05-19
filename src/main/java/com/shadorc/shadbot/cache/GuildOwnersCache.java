package com.shadorc.shadbot.cache;

import discord4j.rest.util.Snowflake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GuildOwnersCache {

    private final Map<Long, Long> map;

    public GuildOwnersCache() {
        this.map = new ConcurrentHashMap<>();
    }

    public void put(Snowflake guildId, Snowflake ownerId) {
        this.map.put(guildId.asLong(), ownerId.asLong());
    }

    public Snowflake get(Snowflake guildId) {
        return Snowflake.of(this.map.get(guildId.asLong()));
    }

}
