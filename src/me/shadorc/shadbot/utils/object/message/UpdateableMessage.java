package me.shadorc.shadbot.utils.object.message;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

public class UpdateableMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final AtomicLong messageId;

	/**
	 * Sends a message that will be deleted each time the {@code send} method is called
	 *
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 */
	public UpdateableMessage(DiscordClient client, Snowflake channelId) {
		this.client = client;
		this.channelId = channelId;
		this.messageId = new AtomicLong(0);
	}

	/**
	 * Send a message and delete the previous one
	 *
	 * @param embed - the embed to send
	 */
	public Mono<Message> send(Consumer<EmbedCreateSpec> embed) {
		return Mono.justOrEmpty(this.messageId.get() == 0 ? null : Snowflake.of(this.messageId.get()))
				.flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
				.flatMap(Message::delete)
				.onErrorResume(ExceptionUtils::isKnownDiscordError, err -> Mono.empty())
				.then(this.client.getChannelById(this.channelId))
				.cast(MessageChannel.class)
				.flatMap(channel -> DiscordUtils.sendMessage(embed, channel))
				.doOnNext(message -> this.messageId.set(message.getId().asLong()));
	}

}
