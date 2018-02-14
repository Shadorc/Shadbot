package me.shadorc.shadbot.utils.object;

import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TextUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.RequestBuffer;
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
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES)) {
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send a message.", channel.getGuild().getLongID());
			return;
		}

		IMessage message = msgRequest == null ? null : msgRequest.get();
		if(message == null) {
			msgRequest = BotUtils.sendMessage(content, channel);
		} else {
			RequestBuffer.request(() -> {
				message.edit(content);
			});
		}
	}

	public void edit(EmbedObject embed) {
		if(!BotUtils.hasPermissions(channel, Permissions.SEND_MESSAGES, Permissions.EMBED_LINKS)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permissions.EMBED_LINKS), channel);
			LogUtils.infof("{Guild ID: %d} Shadbot wasn't allowed to send embed link.", channel.getGuild().getLongID());
		}

		IMessage message = msgRequest == null ? null : msgRequest.get();
		if(message == null) {
			msgRequest = BotUtils.sendMessage(embed, channel);
		} else {
			RequestBuffer.request(() -> {
				message.edit(embed);
			});
		}
	}

	public void delete() {
		IMessage message = msgRequest == null ? null : msgRequest.get();
		if(message != null) {
			RequestBuffer.request(() -> {
				message.delete();
			});
		}
	}

}
