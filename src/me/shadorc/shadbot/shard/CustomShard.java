package me.shadorc.shadbot.shard;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import discord4j.core.DiscordClient;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class CustomShard {

	private final DiscordClient client;
	private final int shardIndex;
	private final AtomicLong lastEvent;
	private final AtomicLong lastMessage;

	private ThreadPoolExecutor threadPool;

	public CustomShard(DiscordClient client) {
		this.client = client;
		this.shardIndex = client.getConfig().getShardIndex();
		this.threadPool = ShardManager.createThreadPool(this);
		this.lastEvent = new AtomicLong();
		this.lastMessage = new AtomicLong();

		Shadbot.scheduleAtFixedRate(() -> this.postStats(), 2, 2, TimeUnit.HOURS);
	}

	public DiscordClient getClient() {
		return client;
	}

	public int getIndex() {
		return shardIndex;
	}

	public int getShardCount() {
		return client.getConfig().getShardCount();
	}

	public Mono<Long> getGuildsCount() {
		return client.getGuilds().count();
	}

	public ThreadPoolExecutor getThreadPool() {
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

	public void postStats() {
		LogUtils.infof("{Shard %d} Posting statistics...", this.getIndex());
		NetUtils.postStatsOn("https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN, this);
		NetUtils.postStatsOn("https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN, this);
		LogUtils.infof("{Shard %d} Statistics posted.", this.getIndex());
	}

}
