package me.shadorc.discordbot.utils.schedule;

import java.util.Objects;

import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class ScheduledMessage {

	private final Object message;
	private final IChannel channel;
	private final Reason reason;

	public enum Reason {
		SHARD_NOT_READY,
		API_ERROR
	}

	public ScheduledMessage(Object message, IChannel channel, Reason reason) {
		this.message = message;
		this.channel = channel;
		this.reason = reason;
	}

	public Object getMessage() {
		return message;
	}

	public IChannel getChannel() {
		return channel;
	}

	public Reason getReason() {
		return reason;
	}

	public IMessage send() {
		if(message instanceof String) {
			return BotUtils.sendMessage((String) message, channel).get();
		} else {
			return BotUtils.sendMessage((EmbedObject) message, channel).get();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		}

		if(!ScheduledMessage.class.isAssignableFrom(obj.getClass())) {
			return false;
		}

		ScheduledMessage otherMsg = (ScheduledMessage) obj;
		return otherMsg.getMessage().equals(message) && otherMsg.getReason().equals(reason);
	}

	@Override
	public int hashCode() {
		return Objects.hash(message, reason);
	}
}
