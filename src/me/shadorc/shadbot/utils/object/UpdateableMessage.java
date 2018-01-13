package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class UpdateableMessage {

	private final IChannel channel;
	private RequestFuture<IMessage> futureMsg;

	public UpdateableMessage(IChannel channel) {
		this.channel = channel;
	}

	public RequestFuture<IMessage> send(EmbedObject embed) {
		if(futureMsg != null) {
			futureMsg.get().delete();
		}

		futureMsg = BotUtils.sendMessage(embed, channel);
		return futureMsg;
	}

}
