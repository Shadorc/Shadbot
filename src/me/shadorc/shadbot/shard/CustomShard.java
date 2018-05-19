package me.shadorc.shadbot.shard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;

public class CustomShard {

	private static final int MAX_QUEUE_SIZE = 20;

	private final DiscordClient client;
	private final int shardIndex;
	private final Map<MessageCreateSpec, MessageChannel> messagesQueue;
	private final AtomicLong lastEvent;
	private final AtomicLong lastMessage;

	private ThreadPoolExecutor threadPool;

	public CustomShard(DiscordClient client) {
		this.client = client;
		this.shardIndex = client.getConfig().getShardIndex();
		this.messagesQueue = new LinkedHashMap<>();
		this.threadPool = ShardManager.createThreadPool(this);
		this.lastEvent = new AtomicLong();
		this.lastMessage = new AtomicLong();
	}

	public DiscordClient getClient() {
		return client;
	}

	public int getIndex() {
		return shardIndex;
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

	public void queue(MessageCreateSpec message, MessageChannel channel) {
		if(messagesQueue.size() < MAX_QUEUE_SIZE) {
			messagesQueue.put(message, channel);
		}
	}

	public void sendQueue() {
		messagesQueue.keySet().stream()
		.forEach(channel -> BotUtils.sendMessage(channel, messagesQueue.get(channel)));
		messagesQueue.clear();
	}

	public void eventReceived() {
		lastEvent.set(System.currentTimeMillis());
	}

	public void messageReceived() {
		lastMessage.set(System.currentTimeMillis());
	}

}
