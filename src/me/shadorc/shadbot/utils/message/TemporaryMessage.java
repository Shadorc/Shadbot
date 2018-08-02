package me.shadorc.shadbot.utils.message;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Mono;

public class TemporaryMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final long delay;
	private final TemporalUnit unit;

	private Snowflake messageId;

	/**
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 * @param delay - the delay to wait
	 * @param unit - the delay unit
	 */
	public TemporaryMessage(DiscordClient client, Snowflake channelId, long delay, TemporalUnit unit) {
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
				.map(message -> this.messageId = message.getId())
				.then(Mono.delay(Duration.of(delay, unit)))
				.then(client.getMessageById(channelId, messageId))
				.flatMap(Message::delete);
	}

}
