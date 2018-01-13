package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class LoadingMessage {

	private final String msg;
	private final IChannel channel;

	private RequestFuture<IMessage> msgRequest;

	public LoadingMessage(String msg, IChannel channel) {
		this.msg = msg;
		this.channel = channel;
	}

	public void send() {
		msgRequest = BotUtils.sendMessage(Emoji.HOURGLASS + " " + msg, channel);
	}

	public void edit(String content) {
		msgRequest.get().edit(content);
	}

	public void edit(EmbedObject embed) {
		msgRequest.get().edit(embed);
	}

	public void delete() {
		msgRequest.get().delete();
	}

}
