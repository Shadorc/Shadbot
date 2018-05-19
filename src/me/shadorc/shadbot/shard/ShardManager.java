package me.shadorc.shadbot.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.executor.CachedWrappedExecutor;

public class ShardManager {

	private static final int SHARD_TIMEOUT = 30;

	private static final Map<DiscordClient, CustomShard> SHARDS_MAP = new HashMap<>();
	private static final ThreadPoolExecutor DEFAULT_THREAD_POOL = new CachedWrappedExecutor("DefaultThreadPool-%d");

	public static void start() {
		Shadbot.scheduleAtFixedRate(() -> ShardManager.check(), 30, 30, TimeUnit.MINUTES);
	}

	public static ThreadPoolExecutor createThreadPool(CustomShard shard) {
		return new CachedWrappedExecutor("ShadbotShard-" + shard.getIndex() + "-%d");
	}

	public static CustomShard getShard(DiscordClient client) {
		return SHARDS_MAP.get(client);
	}

	/**
	 * @param guild - the guild in which the event happened
	 * @param runnable - the runnable to execute
	 * @return true if the runnable could have been executed, false otherwise
	 */
	public static boolean execute(@Nullable Guild guild, Runnable runnable) {
		ThreadPoolExecutor threadPool;

		// Private message
		if(guild == null) {
			threadPool = DEFAULT_THREAD_POOL;
		} else {
			SHARDS_MAP.get(guild.getClient()).eventReceived();
			threadPool = SHARDS_MAP.get(guild.getClient()).getThreadPool();
		}

		if(threadPool.isShutdown()) {
			return false;
		}
		threadPool.execute(runnable);
		return true;
	}

	public static void addShardIfAbsent(DiscordClient client) {
		SHARDS_MAP.putIfAbsent(client, new CustomShard(client));
	}

	private static void check() {
		LogUtils.infof("Checking dead shards...");
		for(CustomShard shard : SHARDS_MAP.values()) {
			try {
				long lastEventTime = TimeUtils.getMillisUntil(shard.getLastEventTime());
				long lastMessageTime = TimeUtils.getMillisUntil(shard.getLastMessageTime());
				if(lastEventTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT) || lastMessageTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
					LogUtils.infof(String.format("Restarting shard %d (Guilds: %d | Last event: %s ago | Last message: %s ago)",
							shard.getIndex(),
							shard.getGuildsCount(),
							DurationFormatUtils.formatDurationWords(lastEventTime, true, true),
							DurationFormatUtils.formatDurationWords(lastMessageTime, true, true)));

					shard.getClient().reconnect();
				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while restarting shard %d.", shard.getIndex()));
			}
		}
		LogUtils.infof("Dead shards checked.");
	}
}
