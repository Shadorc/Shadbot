package me.shadorc.shadbot.core;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.time.DurationFormatUtils;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class CustomShard {

	private static final long SHARD_TIMEOUT = TimeUnit.MINUTES.toSeconds(1);

	private final DiscordClient client;
	private final int shardIndex;
	private final AtomicLong lastMessage;

	public CustomShard(DiscordClient client) {
		this.client = client;
		this.shardIndex = client.getConfig().getShardIndex();
		this.lastMessage = new AtomicLong();

		Shadbot.scheduleAtFixedRate(() -> this.postStats(), 2, 2, TimeUnit.HOURS);
		Shadbot.scheduleAtFixedRate(() -> this.check(), 30, 30, TimeUnit.MINUTES);

		client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> this.messageReceived());
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

	public void messageReceived() {
		lastMessage.set(System.currentTimeMillis());
	}

	public void postStats() {
		LogUtils.infof("{Shard %d} Posting statistics...", this.getIndex());
		NetUtils.postStatsOn("https://bots.discord.pw", APIKey.BOTS_DISCORD_PW_TOKEN, this);
		NetUtils.postStatsOn("https://discordbots.org", APIKey.DISCORD_BOTS_ORG_TOKEN, this);
		LogUtils.infof("{Shard %d} Statistics posted.", this.getIndex());
	}

	private void check() {
		this.getGuildsCount().subscribe(guildsCount -> {
			// Ignore shards containing less than 250 guilds
			if(guildsCount < 250) {
				return;
			}

			try {
				long lastMessageTime = TimeUtils.getMillisUntil(lastMessage.get());
				if(lastMessageTime > TimeUnit.SECONDS.toMillis(SHARD_TIMEOUT)) {
					LogUtils.infof(String.format("Restarting shard %d (Guilds: %d | Last message: %s ago)",
							this.getIndex(), guildsCount, DurationFormatUtils.formatDurationWords(lastMessageTime, true, true)));

					client.reconnect();
				}
			} catch (Exception err) {
				LogUtils.error(client, err, String.format("An error occurred while restarting shard %d.", this.getIndex()));
			}
		});
	}

}
