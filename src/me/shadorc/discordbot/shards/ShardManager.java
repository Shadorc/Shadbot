package me.shadorc.discordbot.shards;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.handle.obj.IGuild;

public class ShardManager {

	private final static int TIMEOUT = 30;

	private final static List<ShardStatus> SHARDS_STATUS = new ArrayList<>();
	private final static ScheduledExecutorService EXECUTOR =
			Executors.newSingleThreadScheduledExecutor(Utils.getThreadFactoryNamed("Shadbot-ShardWatcher"));

	public static void start() {
		for(IShard shard : Shadbot.getClient().getShards()) {
			SHARDS_STATUS.add(new ShardStatus(shard));
		}
		EXECUTOR.scheduleAtFixedRate(() -> ShardManager.check(), 10, 10, TimeUnit.MINUTES);
		LogUtils.info("Shard watcher started.");
	}

	public static void stop() {
		EXECUTOR.shutdownNow();
		LogUtils.info("Shard watcher stopped.");
	}

	public static ExecutorService getThreadPool(IGuild guild) {
		if(guild == null) {
			return Shadbot.getDefaultThreadPool();
		}

		IShard shard = guild.getShard();
		for(ShardStatus shardStatus : SHARDS_STATUS) {
			if(shardStatus.getShard().equals(shard)) {
				shardStatus.alive();
				return shardStatus.getThreadPool();
			}
		}
		LogUtils.error("No ThreadPool found for shard " + shard.getInfo()[0]);
		return null;
	}

	private static void check() {
		LogUtils.info("Checking for dead shards");
		for(ShardStatus shardStatus : SHARDS_STATUS) {
			long lastEventTime = System.currentTimeMillis() - shardStatus.getLastAlive();
			if(lastEventTime > TimeUnit.SECONDS.toMillis(TIMEOUT)) {
				try {
					LogUtils.info(String.format("Restarting shard %d (Response time: %dms | Last event: %d seconds ago)",
							shardStatus.getShardNum(),
							shardStatus.getShard().getResponseTime(),
							lastEventTime / 1000));
					shardStatus.restart();
				} catch (Exception err) {
					LogUtils.info("An error occurred while restarting shard " + shardStatus.getShardNum() + " (" + err.getMessage() + ")");
				}
			}
		}
	}
}
