package me.shadorc.shadbot.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.object.entity.Guild;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.executor.ShadbotCachedExecutor;
import sx.blah.discord.api.IShard;

public class ShardManager {

	private static final int SHARD_TIMEOUT = 30;

	private static final Map<IShard, ShadbotShard> SHARDS_MAP = new HashMap<>();
	private static final ThreadPoolExecutor DEFAUT_THREAD_POOL = new ShadbotCachedExecutor("DefaultThreadPool-%d");

	public static void start() {
		Shadbot.getScheduler().scheduleAtFixedRate(() -> ShardManager.check(), 30, 30, TimeUnit.MINUTES);
	}

	public static ThreadPoolExecutor createThreadPool(ShadbotShard shard) {
		return new ShadbotCachedExecutor("ShadbotShard-" + shard.getID() + "-%d");
	}

	public static ShadbotShard getShadbotShard(IShard shard) {
		return SHARDS_MAP.get(shard);
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
			SHARDS_MAP.get(guild.getShard()).eventReceived();
			threadPool = SHARDS_MAP.get(guild.getShard()).getThreadPool();
		}

		if(threadPool.isShutdown()) {
			return false;
		}
		threadPool.execute(runnable);
		return true;
	}

	public static void addShardIfAbsent(IShard shard) {
		SHARDS_MAP.putIfAbsent(shard, new ShadbotShard(shard));
	}

	private static void check() {
		LogUtils.infof("Checking dead shards...");
		for(ShadbotShard shardStatus : SHARDS_MAP.values()) {
			try {
				// Ignore small shards
				if(shardStatus.getShard().getGuilds().size() < 250) {
					continue;
				}

				// Don't restart a shard if it's already restarting
				if(shardStatus.isRestarting()) {
					continue;
				}
				long lastEventTime = TimeUtils.getMillisUntil(shardStatus.getLastEventTime());
				long lastMessageTime = TimeUtils.getMillisUntil(shardStatus.getLastMessageTime());
				if(lastEventTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT) || lastMessageTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
					LogUtils.infof(String.format("Restarting shard %d "
							+ "(Guilds: %d | Response time: %d ms | Last event: %s ago | Last message: %s ago)",
							shardStatus.getID(),
							shardStatus.getShard().getGuilds().size(),
							shardStatus.getShard().getResponseTime(),
							DurationFormatUtils.formatDurationWords(lastEventTime, true, true),
							DurationFormatUtils.formatDurationWords(lastMessageTime, true, true)));

					Future<Boolean> restartFuture = DEFAUT_THREAD_POOL.submit(() -> shardStatus.restart());
					if(!restartFuture.get(1, TimeUnit.MINUTES)) {
						LogUtils.infof("Restart task seems stuck. Restart client ?");
						restartFuture.cancel(true);
						// Shadbot.getClient().logout();
						// Shadbot.getClient().login();
						// break;
					}

				}
			} catch (Exception err) {
				LogUtils.error(err, String.format("An error occurred while restarting shard %d.", shardStatus.getID()));
			}
		}
		LogUtils.infof("Dead shards checked.");
	}
}
