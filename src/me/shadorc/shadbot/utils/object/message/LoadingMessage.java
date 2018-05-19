package me.shadorc.shadbot.utils.object.message;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import discord4j.core.spec.MessageEditSpec;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class LoadingMessage {

	private final Snowflake channelId;
	private MessageCreateSpec messageSpec;
	private Snowflake messageId;

	public LoadingMessage(String content, Snowflake channelId) {
		this.messageSpec = new MessageCreateSpec().setContent(Emoji.HOURGLASS + " " + content);
		this.channelId = channelId;
		this.messageId = null;
	}

	public void send() {
		this.getChannel()
				.subscribe(channel -> BotUtils.sendMessage(messageSpec, channel)
						.subscribe(message -> this.messageId = message.getId()));
	}

	public void edit(String content) {
		messageSpec.setContent(content);

		// If an error occurred, we send a new message instead of trying to edit the previous one
		this.getMessage().doOnError(error -> this.send())
				.subscribe(message -> message.edit(new MessageEditSpec().setContent(content)));
	}

	public void edit(EmbedCreateSpec embed) {
		messageSpec.setEmbed(embed);

		// If an error occurred, we send a new message instead of trying to edit the previous one
		this.getMessage().doOnError(error -> this.send())
				.subscribe(message -> message.edit(new MessageEditSpec().setEmbed(embed)));
	}

	public void delete() {
		this.getMessage().subscribe(Message::delete);
	}

	private Mono<Message> getMessage() {
		return Shadbot.getClient().getMessageById(channelId, messageId)
				.doOnError(err -> LogUtils.error(err, String.format("{%s} An error occurred while getting message.", this.getClass().getSimpleName())));
	}

	private Mono<MessageChannel> getChannel() {
		return Shadbot.getClient().getMessageChannelById(channelId)
				.doOnError(err -> LogUtils.error(err, String.format("{%s} An error occurred while getting channel.", this.getClass().getSimpleName())));
	}

}
