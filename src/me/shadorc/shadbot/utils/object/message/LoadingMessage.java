package me.shadorc.shadbot.utils.object.message;

import java.time.Duration;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateSpec;
import me.shadorc.shadbot.utils.BotUtils;

public class LoadingMessage implements Publisher<Void> {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final Duration typingTimeout;

	private Subscriber<? super Void> subscriber;

	/**
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 * @param typingTimeout - the duration before a message is send Start typing until a message is send or the typing timeout have passed
	 */
	public LoadingMessage(DiscordClient client, Snowflake channelId, Duration typingTimeout) {
		this.client = client;
		this.channelId = channelId;
		this.typingTimeout = typingTimeout;

		this.startTyping();
	}

	/**
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message Start typing until a message is send Start typing until a message is send or 30 seconds
	 *            have passed
	 */
	public LoadingMessage(DiscordClient client, Snowflake channelId) {
		this(client, channelId, Duration.ofSeconds(30));
	}

	/**
	 * Start typing in the channel until a message is send or the typing timeout seconds have passed
	 */
	private void startTyping() {
		client.getMessageChannelById(channelId).subscribe(channel -> channel.typeUntil(this).timeout(typingTimeout));
	}

	/**
	 * Stop typing
	 */
	public void stopTyping() {
		subscriber.onComplete();
	}

	/**
	 * Send a message and stop typing when the message has been send or an error occurred
	 */
	public void send(String content) {
		client.getMessageChannelById(channelId)
				.subscribe(channel -> BotUtils.sendMessage(new MessageCreateSpec().setContent(content), channel)
						.doAfterTerminate(() -> subscriber.onComplete())
						.subscribe());
	}

	/**
	 * Send a message and stop typing when the message has been send or an error occurred
	 */
	public void send(EmbedCreateSpec embed) {
		client.getMessageChannelById(channelId)
				.subscribe(channel -> BotUtils.sendMessage(new MessageCreateSpec().setEmbed(embed), channel)
						.doAfterTerminate(() -> subscriber.onComplete())
						.subscribe());
	}

	@Override
	public void subscribe(Subscriber<? super Void> subscriber) {
		this.subscriber = subscriber;
	}

}
