package me.shadorc.shadbot.shard;

import java.util.Queue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.EvictingQueue;

import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.IShard;
import sx.blah.discord.util.MessageBuilder;

public class ShadbotShard {

	private final IShard shard;
	private final int shardID;
	private final Queue<MessageBuilder> messagesQueue;
	private final AtomicLong lastEvent;
	private final AtomicLong lastMessage;

	private ThreadPoolExecutor threadPool;
	private boolean isRestarting;

	public ShadbotShard(IShard shard) {
		this.shard = shard;
		this.shardID = shard.getInfo()[0];
		this.messagesQueue = EvictingQueue.create(20);
		this.threadPool = ShardManager.createThreadPool(this);
		this.lastEvent = new AtomicLong();
		this.lastMessage = new AtomicLong();
		this.isRestarting = false;
	}

	public IShard getShard() {
		return shard;
	}

	public int getID() {
		return shardID;
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

	public boolean isRestarting() {
		return isRestarting;
	}

	public void queue(MessageBuilder message) {
		messagesQueue.add(message);
	}

	public void sendQueue() {
		messagesQueue.stream().forEach(BotUtils::sendMessage);
		messagesQueue.clear();
	}

	public void eventReceived() {
		lastEvent.set(System.currentTimeMillis());
	}

	public void messageReceived() {
		lastMessage.set(System.currentTimeMillis());
	}

	public void restart() {
		this.isRestarting = true;

		LogUtils.infof("{Shard %d} Restarting...", this.getID());

		GuildMusicManager.GUILD_MUSIC_MAP.values().stream()
				.filter(guildMusic -> guildMusic.getChannel().getShard().equals(this.getShard()))
				.forEach(guildMusic -> {
					LogUtils.infof("{Shard %d | Guild ID: %d} Leaving voice channel...",
							this.getID(), guildMusic.getChannel().getGuild().getLongID());
					guildMusic.leaveVoiceChannel();
				});

		LogUtils.infof("{Shard %d} Logging out...", this.getID());
		if(shard.isLoggedIn()) {
			shard.logout();
		}

		LogUtils.infof("{Shard %d} Thread pool shutdown, %d tasks cancelled.", this.getID(), threadPool.shutdownNow().size());
		try {
			if(!threadPool.awaitTermination(10, TimeUnit.SECONDS)) {
				LogUtils.infof("{Shard %d} Thread pool was abruptly shut down.", this.getID());
			}
		} catch (InterruptedException e) {
			LogUtils.infof("{Shard %d} Thread was interrupted, thread pool was abruptly shut down.", this.getID());
		}

		LogUtils.infof("{Shard %d} Logging in...", this.getID());
		shard.login();
		LogUtils.infof("{Shard %d} Shard restarted.", this.getID());

		threadPool = ShardManager.createThreadPool(this);

		// Reset timeout counter
		this.eventReceived();
		this.messageReceived();

		this.isRestarting = false;
	}
}
