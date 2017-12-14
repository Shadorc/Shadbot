package me.shadorc.discordbot.shards;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.IShard;

public class ShardStatus {

	private final IShard shard;
	private final ExecutorService threadPool;
	private final AtomicLong lastAlive;

	public ShardStatus(IShard shard) {
		this.shard = shard;
		this.threadPool = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors() * 4,
				0, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), Utils.getThreadFactoryNamed("Shadbot-Shard" + this.getShardNum() + "-%d"));
		this.lastAlive = new AtomicLong();
	}

	public IShard getShard() {
		return shard;
	}

	public long getLastAlive() {
		return lastAlive.get();
	}

	public final int getShardNum() {
		return shard.getInfo()[0];
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public void alive() {
		lastAlive.set(System.currentTimeMillis());
	}

	public void restart() {
		shard.logout();
		shard.login();
	}
}
