package me.shadorc.shadbot.listener;

import java.util.concurrent.TimeUnit;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.rest.http.client.ClientException;
import me.shadorc.shadbot.core.exception.ExceptionHandler;
import me.shadorc.shadbot.core.exception.ExceptionUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import reactor.core.publisher.Mono;

public class MessageUpdateListener {

	public static Mono<Void> onMessageUpdateEvent(MessageUpdateEvent event) {
		if(!event.isContentChanged() || !event.getOld().isPresent() || !event.getGuildId().isPresent()) {
			return Mono.empty();
		}

		// If the message has been sent more than 30 seconds ago, ignore it
		final Message oldMessage = event.getOld().get();
		if(TimeUtils.getMillisUntil(oldMessage.getTimestamp()) > TimeUnit.SECONDS.toMillis(30)) {
			return Mono.empty();
		}

		final Snowflake guildId = event.getGuildId().get();
		return Mono.zip(event.getMessage(), event.getMessage().flatMap(Message::getAuthorAsMember))
				.onErrorResume(ExceptionUtils::isNotFound,
						err -> ExceptionHandler.onNotFound((ClientException) err, guildId).then(Mono.empty()))
				.doOnNext(tuple -> {
					final Message message = tuple.getT1();
					final Member member = tuple.getT2();
					MessageCreateListener.onMessageCreate(new MessageCreateEvent(event.getClient(), message, guildId.asLong(), member));
				})
				.then();
	}

}
