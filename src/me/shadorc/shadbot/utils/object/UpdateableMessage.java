package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuffer.RequestFuture;

public class UpdateableMessage {

	private final IChannel channel;
	private RequestFuture<IMessage> futureMsg;

	public UpdateableMessage(IChannel channel) {
		this.channel = channel;
	}

	public RequestFuture<IMessage> send(EmbedObject embed) {
		if(futureMsg != null) {
			if(BotUtils.hasPermissions(channel, Permissions.MANAGE_MESSAGES)) {
				futureMsg.get().delete();
			} else {
				LogUtils.infof("{%d} Shadbot wasn't allowed to delete message.", channel.getGuild().getLongID());
			}
		}

		futureMsg = BotUtils.sendMessage(embed, channel);
		return futureMsg;
	}

	public RequestFuture<IMessage> send(String message) {
		if(futureMsg != null) {
			if(BotUtils.hasPermissions(channel, Permissions.MANAGE_MESSAGES)) {
				futureMsg.get().delete();
			} else {
				LogUtils.infof("{%d} Shadbot wasn't allowed to delete message.", channel.getGuild().getLongID());
			}
		}

		futureMsg = BotUtils.sendMessage(message, channel);
		return futureMsg;
	}

}
