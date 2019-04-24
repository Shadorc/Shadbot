package me.shadorc.shadbot.core.ratelimiter;

import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket4j;

import java.util.concurrent.ConcurrentHashMap;

public class LimitedGuild {

    private final ConcurrentHashMap<Snowflake, LimitedUser> limitedUsersMap;
    private final Bandwidth bandwidth;

    public LimitedGuild(Bandwidth bandwidth) {
        this.limitedUsersMap = new ConcurrentHashMap<>();
        this.bandwidth = bandwidth;
    }

    public LimitedUser getUser(Snowflake userId) {
        return this.limitedUsersMap.computeIfAbsent(userId,
                id -> new LimitedUser(Bucket4j.builder().addLimit(this.bandwidth).build()));
    }

}
