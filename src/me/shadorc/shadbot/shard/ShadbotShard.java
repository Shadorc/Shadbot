package me.shadorc.shadbot.shard;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import sx.blah.discord.api.IShard;

public class ShadbotShard {

	private final IShard shard;
	private final int shardID;
	private ExecutorService threadPool;
	private final AtomicLong lastEvent;
	private final AtomicLong lastMessage;

	public ShadbotShard(IShard shard) {
		this.shard = shard;
		this.shardID = shard.getInfo()[0];
		this.threadPool = ShardManager.createThreadPool(this);
		this.lastEvent = new AtomicLong();
		this.lastMessage = new AtomicLong();
	}

	public IShard getShard() {
		return shard;
	}

	public int getID() {
		return shardID;
	}

	public ExecutorService getThreadPool() {
		return threadPool;
	}

	public long getLastEventTime() {
		return lastEvent.get();
	}

	public long getLastMessageTime() {
		return lastMessage.get();
	}

	public void eventReceived() {
		lastEvent.set(System.currentTimeMillis());
	}

	public void messageReceived() {
		lastMessage.set(System.currentTimeMillis());
	}

	public void restart() {
		shard.logout();
		threadPool.shutdownNow();
		shard.login();
		threadPool = ShardManager.createThreadPool(this);
	}
}
