package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
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
		if(!BotUtils.hasPermissions(channel, Permissions.MANAGE_MESSAGES)) {
			LogUtils.infof("{%d} Shadbot wasn't allowed to edit message.", channel.getGuild().getLongID());
			return;
		}
		msgRequest.get().edit(content);
	}

	// TODO: Do we need to check manage message to edit a message ?
	public void edit(EmbedObject embed) {
		if(!BotUtils.hasPermissions(channel, Permissions.MANAGE_MESSAGES)) {
			LogUtils.infof("{%d} Shadbot wasn't allowed to edit message.", channel.getGuild().getLongID());
			return;
		}
		msgRequest.get().edit(embed);
	}

	public void delete() {
		if(!BotUtils.hasPermissions(channel, Permissions.MANAGE_MESSAGES)) {
			LogUtils.infof("{%d} Shadbot wasn't allowed to delete message.", channel.getGuild().getLongID());
			return;
		}
		msgRequest.get().delete();
	}

}
