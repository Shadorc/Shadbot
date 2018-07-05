package me.shadorc.shadbot.listener.interceptor;

import discord4j.core.event.domain.message.MessageCreateEvent;

public interface MessageInterceptor {

	/**
	 * @param event - the event to intercept
	 * @return true if the event has been intercepted (blocking the execution of other commands), false otherwise
	 */
	boolean isIntercepted(MessageCreateEvent event);

}
