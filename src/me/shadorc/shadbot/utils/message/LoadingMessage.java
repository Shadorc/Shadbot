package me.shadorc.shadbot.utils.message;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class LoadingMessage implements Publisher<Void> {

	private final DiscordClient client;
	private final Snowflake channelId;
	private final Duration typingTimeout;

	private final List<Subscriber<? super Void>> subscribers;

	/**
	 * Start typing until a message is send or the typing timeout have passed
	 * 
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 * @param typingTimeout - the duration before a message is send
	 */
	public LoadingMessage(DiscordClient client, Snowflake channelId, Duration typingTimeout) {
		this.client = client;
		this.channelId = channelId;
		this.typingTimeout = typingTimeout;
		this.subscribers = new ArrayList<>();

		this.startTyping()
				.doOnError(ExceptionHandler::isForbidden, err -> LogUtils.cannotSpeak(this.getClass()))
				.subscribe();
	}

	/**
	 * Start typing until a message is send Start typing until a message is send or 30 seconds have passed
	 * 
	 * @param client - the Discord client
	 * @param channelId - the Channel ID in which send the message
	 */
	public LoadingMessage(DiscordClient client, Snowflake channelId) {
		this(client, channelId, Duration.ofSeconds(30));
	}

	/**
	 * Start typing in the channel until a message is send or the typing timeout seconds have passed
	 */
	private Flux<Long> startTyping() {
		return client.getMessageChannelById(channelId)
				.flatMapMany(channel -> channel.typeUntil(this))
				.take(typingTimeout);
	}

	/**
	 * Stop typing
	 */
	public void stopTyping() {
		subscribers.forEach(Subscriber::onComplete);
	}

	/**
	 * Send a message and stop typing when the message has been send or an error occurred
	 */
	public Mono<Message> send(String content) {
		return BotUtils.sendMessage(content, client.getMessageChannelById(channelId))
				.doAfterTerminate(this::stopTyping);
	}

	/**
	 * Send a message and stop typing when the message has been send or an error occurred
	 */
	public Mono<Message> send(EmbedCreateSpec embed) {
		return BotUtils.sendMessage(embed, client.getMessageChannelById(channelId))
				.doAfterTerminate(this::stopTyping);
	}

	@Override
	public void subscribe(Subscriber<? super Void> subscriber) {
		subscribers.add(subscriber);
	}

}
