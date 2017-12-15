package me.shadorc.discordbot.shards;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.IShard;

public class ShardStatus {

	private final IShard shard;
	private final ExecutorService threadPool;
	private final AtomicLong lastAlive;

	public ShardStatus(IShard shard) {
		this.shard = shard;
		this.threadPool = Executors.newCachedThreadPool(Utils.getThreadFactoryNamed("Shadbot-Shard-" + this.getShardNum() + "-%d"));
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
