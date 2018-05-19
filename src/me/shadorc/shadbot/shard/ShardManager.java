package me.shadorc.shadbot.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Guild;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.executor.CachedWrappedExecutor;

public class ShardManager {

	private static final int SHARD_TIMEOUT = 30;

	private static final Map<DiscordClient, CustomShard> SHARDS_MAP = new HashMap<>();
	private static final ThreadPoolExecutor DEFAUT_THREAD_POOL = new CachedWrappedExecutor("DefaultThreadPool-%d");

	public static void start() {
		Shadbot.getScheduler().scheduleAtFixedRate(() -> ShardManager.check(), 30, 30, TimeUnit.MINUTES);
	}

	public static ThreadPoolExecutor createThreadPool(CustomShard shard) {
		return new CachedWrappedExecutor("ShadbotShard-" + shard.getIndex() + "-%d");
	}

	public static CustomShard getShadbotShard(DiscordClient client) {
		return SHARDS_MAP.get(client);
	}

	/**
	 * @param guild - the guild in which the event happened (can be null)
	 * @param runnable - the runnable to execute
	 * @return true if the runnable could have been executed, false otherwise
	 */
	public static boolean execute(Guild guild, Runnable runnable) {
		ThreadPoolExecutor threadPool;

		// Private message
		if(guild == null) {
			threadPool = DEFAUT_THREAD_POOL;
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
				// Ignore small shards
				// TODO: Does getGuilds() return the total number of guilds
				// or just the one that are connected to the client ?
				if(shard.getClient().getGuilds().count().block() < 250) {
					continue;
				}

				long lastEventTime = TimeUtils.getMillisUntil(shard.getLastEventTime());
				long lastMessageTime = TimeUtils.getMillisUntil(shard.getLastMessageTime());
				if(lastEventTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT) || lastMessageTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
					LogUtils.infof(String.format("Restarting shard %d "
							+ "(Guilds: %d | Last event: %s ago | Last message: %s ago)",
							shard.getIndex(),
							shard.getClient().getGuilds().count().block(),
							DurationFormatUtils.formatDurationWords(lastEventTime, true, true),
							DurationFormatUtils.formatDurationWords(lastMessageTime, true, true)));

					Future<Void> restartFuture = DEFAUT_THREAD_POOL.submit(() -> shard.getClient().reconnect());
					if(!restartFuture.get(1, TimeUnit.MINUTES)) {
						LogUtils.infof("Restart task seems stuck. Restart client ?");
						restartFuture.cancel(true);
						// Shadbot.getClient().logout();
						// Shadbot.getClient().login();
						// break;
					}

				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while restarting shard %d.", shard.getID()));
			}
		}
		LogUtils.infof("Dead shards checked.");
	}
}
