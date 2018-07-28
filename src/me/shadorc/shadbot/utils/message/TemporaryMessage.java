package me.shadorc.shadbot.utils.message;

import java.util.concurrent.TimeUnit;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Mono;

public class TemporaryMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final long delay;
	private final TimeUnit unit;

	/**
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 * @param delay - the delay to wait
	 * @param unit - the delay unit
	 */
	public TemporaryMessage(DiscordClient client, Snowflake channelId, long delay, TimeUnit unit) {
		this.client = client;
		this.channelId = channelId;
		this.delay = delay;
		this.unit = unit;
	}

	/**
	 * Send a message and then wait {@code delay} {@code unit} to delete it
	 * 
	 * @param content - the message's content
	 * @return A Mono representing the message sent
	 */
	public Mono<Void> send(String content) {
		return BotUtils.sendMessage(content, client.getMessageChannelById(channelId))
				.flatMap(message -> Shadbot.getScheduler().schedule(message.delete(), delay, unit))
				.then();
	}

}
