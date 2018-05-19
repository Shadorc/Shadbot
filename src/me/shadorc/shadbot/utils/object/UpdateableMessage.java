package me.shadorc.shadbot.utils.object;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;
import reactor.core.publisher.Mono;

public class UpdateableMessage {

	private final TextChannel channel;
	private Mono<Message> message;

	public UpdateableMessage(TextChannel channel) {
		this.channel = channel;
		this.message = Mono.empty();
	}

	public Mono<Message> send(EmbedCreateSpec embed) {
		message.blockOptional().ifPresent(Message::delete);
		message = BotUtils.sendMessage(embed, channel);
		return message;
	}

}
