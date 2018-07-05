package me.shadorc.shadbot.listener.interceptor;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Snowflake;

public class MessageInterceptorManager {

	private static final Multimap<Snowflake, MessageInterceptor> CHANNELS_INTERCEPTORS = Multimaps.synchronizedMultimap(HashMultimap.create());

	public static void addInterceptor(Snowflake channelId, MessageInterceptor interceptor) {
		CHANNELS_INTERCEPTORS.put(channelId, interceptor);
	}

	public static void removeInterceptor(Snowflake channelId, MessageInterceptor interceptor) {
		Collection<MessageInterceptor> listeners = CHANNELS_INTERCEPTORS.get(channelId);
		listeners.remove(interceptor);
		if(listeners.isEmpty()) {
			CHANNELS_INTERCEPTORS.removeAll(channelId);
		}
	}

	/**
	 * @param event - the event to intercept
	 * @return true if the event has been intercepted (blocking the execution of other commands), false otherwise
	 */
	public static boolean isIntercepted(MessageCreateEvent event) {
		return CHANNELS_INTERCEPTORS.get(event.getMessage().getChannelId())
				.stream()
				.map(interceptor -> interceptor.isIntercepted(event))
				.anyMatch(Boolean.TRUE::equals);
	}
}
