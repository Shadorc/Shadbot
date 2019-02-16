package me.shadorc.shadbot.utils.object.message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LoadingMessage implements Publisher<Void> {

	private final static Duration TYPING_TIMEOUT = Duration.ofSeconds(30);

	private final DiscordClient client;
	private final Snowflake channelId;
	private final List<Subscriber<? super Void>> subscribers;

	/**
	 * Start typing until a message is send or 30 seconds have passed
	 *
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 */
	public LoadingMessage(DiscordClient client, Snowflake channelId) {
		this.client = client;
		this.channelId = channelId;
		this.subscribers = new ArrayList<>();

		this.startTyping()
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(client, err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(client, err));
	}

	/**
	 * Start typing in the channel until a message is send or the typing timeout have passed
	 */
	private Flux<Long> startTyping() {
		return this.client.getChannelById(this.channelId)
				.cast(MessageChannel.class)
				.filterWhen(channel -> DiscordUtils.hasPermission(channel, this.client.getSelfId().get(), Permission.SEND_MESSAGES))
				.flatMapMany(channel -> channel.typeUntil(this))
				.take(TYPING_TIMEOUT);
	}

	/**
	 * Stop typing
	 */
	public void stopTyping() {
		this.subscribers.forEach(Subscriber::onComplete);
	}

	/**
	 * Send a message and stop typing when the message has been sent or an error occurred
	 */
	public Mono<Message> send(String content) {
		return this.client.getChannelById(this.channelId)
				.cast(MessageChannel.class)
				.flatMap(channel -> DiscordUtils.sendMessage(content, channel))
				.timeout(TYPING_TIMEOUT)
				.doAfterTerminate(this::stopTyping);
	}

	/**
	 * Send a message and stop typing when the message has been sent or an error occurred
	 */
	public Mono<Message> send(Consumer<EmbedCreateSpec> embed) {
		return this.client.getChannelById(this.channelId)
				.cast(MessageChannel.class)
				.flatMap(channel -> DiscordUtils.sendMessage(embed, channel))
				.timeout(TYPING_TIMEOUT)
				.doAfterTerminate(this::stopTyping);
	}

	@Override
	public void subscribe(Subscriber<? super Void> subscriber) {
		this.subscribers.add(subscriber);
	}

}
