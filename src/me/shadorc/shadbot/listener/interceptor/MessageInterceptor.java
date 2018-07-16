package me.shadorc.shadbot.listener.interceptor;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public interface MessageInterceptor {

	/**
	 * @param event - the event to intercept
	 * @return true if the event has been intercepted (blocking the execution of other commands), false otherwise
	 */
	Mono<Boolean> isIntercepted(MessageCreateEvent event);

}
