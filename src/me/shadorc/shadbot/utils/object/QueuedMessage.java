package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;

public class QueuedMessage {

	private final IChannel channel;
	private final EmbedObject embed;
	private final String message;

	public QueuedMessage(IChannel channel, EmbedObject embed) {
		this.channel = channel;
		this.embed = embed;
		this.message = null;
	}

	public QueuedMessage(IChannel channel, String message) {
		this.channel = channel;
		this.message = message;
		this.embed = null;
	}

	public void send() {
		if(embed == null) {
			BotUtils.sendMessage(message, channel);
		} else {
			BotUtils.sendMessage(embed, channel);
		}
	}

}
