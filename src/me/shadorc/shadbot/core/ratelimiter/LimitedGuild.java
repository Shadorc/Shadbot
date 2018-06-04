package me.shadorc.shadbot.core.ratelimiter;

import java.util.concurrent.ConcurrentHashMap;

import discord4j.core.object.util.Snowflake;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;

public class LimitedGuild {

	private final ConcurrentHashMap<Snowflake, Bucket> limitedUsersMap;
	private final Bandwidth bandwidth;

	public LimitedGuild(Bandwidth bandwidth) {
		this.limitedUsersMap = new ConcurrentHashMap<>();
		this.bandwidth = bandwidth;
	}

	public Bucket getUserBucket(Snowflake userId) {
		limitedUsersMap.putIfAbsent(userId, Bucket4j.builder().addLimit(this.bandwidth).build());
		return limitedUsersMap.get(userId);
	}

}
