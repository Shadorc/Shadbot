package me.shadorc.shadbot.utils.message;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;

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
	public void send(EmbedCreateSpec embed) {
		// Delete the previous message
		client.getMessageById(channelId, messageId)
				.subscribe(Message::delete);
		// Send the new one and store its messageId
		BotUtils.sendMessage(embed, client.getMessageChannelById(channelId))
				.map(Message::getId)
				.subscribe(messageId -> this.messageId = messageId);
	}

}
