package me.shadorc.shadbot.utils.object.message;

import java.util.Optional;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Mono;

public class UpdateableMessage {

	private final DiscordClient client;
	private final Snowflake channelId;
	private Snowflake messageId;

	/**
	 * Sends a message that will be deleted each time the {@code send} method is called
	 *
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 */
	public UpdateableMessage(DiscordClient client, Snowflake channelId) {
		this.client = client;
		this.channelId = channelId;
		this.messageId = null;
	}

	/**
	 * Send a message and delete the previous one
	 *
	 * @param embed - the embed to send
	 */
	public Mono<Message> send(EmbedCreateSpec embed) {
		return Mono.justOrEmpty(Optional.ofNullable(this.messageId))
				.flatMap(messageId -> this.client.getMessageById(this.channelId, messageId))
				.flatMap(Message::delete)
				.then(BotUtils.sendMessage(embed, this.client.getMessageChannelById(this.channelId)))
				.doOnSuccess(message -> this.messageId = message.getId());
	}

}
