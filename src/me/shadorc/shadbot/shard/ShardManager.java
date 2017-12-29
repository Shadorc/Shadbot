package me.shadorc.shadbot.shard;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.LogUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.obj.IGuild;

public class ShardManager {

	private final static int SHARD_TIMEOUT = 30;

	private final static Map<IShard, ShadbotShard> SHARDS_MAP = new HashMap<>();
	private final static ScheduledExecutorService EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(Utils.getThreadFactoryNamed("Shadbot-ShardWatcher"));
	private static final ExecutorService DEFAUT_THREAD_POOL =
			Executors.newCachedThreadPool(Utils.getThreadFactoryNamed("Shadbot-DefaultThreadPool-%d"));

	public static void start() {
		EXECUTOR.scheduleAtFixedRate(() -> ShardManager.check(), 10, 10, TimeUnit.MINUTES);
	}

	public static void stop() {
		SHARDS_MAP.keySet().stream().forEach(shard -> shard.logout());
		EXECUTOR.shutdownNow();
	}

	public static ShadbotShard getShadbotShard(IShard shard) {
		return SHARDS_MAP.get(shard);
	}

	public static void addShardIfAbsent(IShard shard) {
		SHARDS_MAP.putIfAbsent(shard, new ShadbotShard(shard));
	}

	public static ExecutorService getThreadPool(IGuild guild) {
		// Private message
		if(guild == null) {
			return DEFAUT_THREAD_POOL;
		}

		SHARDS_MAP.get(guild.getShard()).used();
		return SHARDS_MAP.get(guild.getShard()).getThreadPool();
	}

	private static void check() {
		LogUtils.infof("Checking dead shards...");
		for(ShadbotShard shardStatus : SHARDS_MAP.values()) {
			long lastEventTime = System.currentTimeMillis() - shardStatus.getLastUsed();
			if(lastEventTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
				LogUtils.infof(String.format("Restarting shard %d (Response time: %d ms | Last event: %d seconds ago)",
						shardStatus.getID(),
						shardStatus.getShard().getResponseTime(),
						lastEventTime / 1000));
				shardStatus.restart();
			}
		}
		LogUtils.infof("Dead shards checked.");
	}
}
