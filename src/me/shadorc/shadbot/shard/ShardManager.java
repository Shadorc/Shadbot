package me.shadorc.shadbot.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.DurationFormatUtils;

import me.shadorc.shadbot.utils.DateUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.ThreadPoolUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.obj.IGuild;

public class ShardManager {

	private final static int SHARD_TIMEOUT = 30;

	private final static Map<IShard, ShadbotShard> SHARDS_MAP = new HashMap<>();
	private final static ScheduledExecutorService EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.getThreadFactoryNamed("Shadbot-ShardWatcher"));
	private static final ExecutorService DEFAUT_THREAD_POOL =
			Executors.newCachedThreadPool(ThreadPoolUtils.getThreadFactoryNamed("Shadbot-DefaultThreadPool-%d"));

	public static void start() {
		EXECUTOR.scheduleAtFixedRate(() -> ShardManager.check(), 10, 10, TimeUnit.MINUTES);
	}

	public static void stop() {
		SHARDS_MAP.values().stream().forEach(shadbotShard -> shadbotShard.getThreadPool().shutdownNow());
		EXECUTOR.shutdownNow();
		DEFAUT_THREAD_POOL.shutdownNow();
	}

	public static ExecutorService createThreadPool(ShadbotShard shard) {
		return Executors.newCachedThreadPool(ThreadPoolUtils.getThreadFactoryNamed("ShadbotShard-" + shard.getID() + "-%d"));
	}

	public static ShadbotShard getShadbotShard(IShard shard) {
		return SHARDS_MAP.get(shard);
	}

	public static ExecutorService getThreadPool(IGuild guild) {
		// Private message
		if(guild == null) {
			return DEFAUT_THREAD_POOL;
		}

		SHARDS_MAP.get(guild.getShard()).eventReceived();
		return SHARDS_MAP.get(guild.getShard()).getThreadPool();
	}

	public static void addShardIfAbsent(IShard shard) {
		SHARDS_MAP.putIfAbsent(shard, new ShadbotShard(shard));
	}

	private static void check() {
		LogUtils.infof("Checking dead shards...");
		for(ShadbotShard shardStatus : SHARDS_MAP.values()) {
			long lastEventTime = DateUtils.getMillisUntil(shardStatus.getLastEventTime());
			long lastMessageTime = DateUtils.getMillisUntil(shardStatus.getLastMessageTime());
			if(lastEventTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT) || lastMessageTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
				LogUtils.infof(String.format("Restarting shard %d (Response time: %d ms | Last event: %s ago | Last message: %s ago)",
						shardStatus.getID(),
						shardStatus.getShard().getResponseTime(),
						DurationFormatUtils.formatDurationWords(lastEventTime, true, true),
						DurationFormatUtils.formatDurationWords(lastMessageTime, true, true)));
				shardStatus.restart();
			}
		}
		LogUtils.infof("Dead shards checked.");
	}
}
