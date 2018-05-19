package me.shadorc.shadbot.utils.object;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.TextUtils;
import reactor.core.publisher.Mono;

public class LoadingMessage {

	private final String content;
	private final MessageChannel channel;

	private Mono<Message> message;

	public LoadingMessage(String content, MessageChannel channel) {
		this.content = content;
		this.channel = channel;
		this.message = Mono.empty();
	}

	public void send() {
		message = BotUtils.sendMessage(Emoji.HOURGLASS + " " + content, channel);
	}

	public void edit(String content) {
		if(!BotUtils.hasPermissions(channel, Permission.SEND_MESSAGES)) {
			LogUtils.infof("{Channel ID: %d} Shadbot wasn't allowed to send a message.", channel.getId().asLong());
			return;
		}

		message.switchIfEmpty(BotUtils.sendMessage(content, channel))
				.block()
				.edit(new MessageEditSpec().setContent(content));
	}

	public void edit(EmbedCreateSpec embed) {
		if(!BotUtils.hasPermissions(channel, Permission.SEND_MESSAGES, Permission.EMBED_LINKS)) {
			BotUtils.sendMessage(TextUtils.missingPerm(Permission.EMBED_LINKS), channel);
			LogUtils.infof("{Channel ID: %d} Shadbot wasn't allowed to send embed link.", channel.getId().asLong());
			return;
		}

		message.switchIfEmpty(BotUtils.sendMessage(embed, channel))
				.block()
				.edit(new MessageEditSpec().setEmbed(embed));
	}

	public void delete() {
		message.blockOptional().ifPresent(Message::delete);
	}

}
