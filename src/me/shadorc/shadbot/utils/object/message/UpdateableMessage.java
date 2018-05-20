package me.shadorc.shadbot.utils.object.message;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.BotUtils;

public class UpdateableMessage {

	private final Snowflake channelId;
	private Snowflake messageId;

	public UpdateableMessage(Snowflake channelId) {
		this.channelId = channelId;
		this.messageId = null;
	}

	public void send(EmbedCreateSpec embed) {
		Shadbot.getClient().getMessageById(channelId, messageId).subscribe(Message::delete);
		Shadbot.getClient().getMessageChannelById(channelId)
				.subscribe(channel -> BotUtils.sendMessage(new MessageCreateSpec().setEmbed(embed), channel)
						.subscribe(message -> this.messageId = message.getId()));
	}

}
