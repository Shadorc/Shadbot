package me.shadorc.shadbot.listener.interceptor;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MessageInterceptorManager {

	private static final Multimap<Snowflake, MessageInterceptor> CHANNELS_INTERCEPTORS = HashMultimap.create();

	public static void addInterceptor(Snowflake channelId, MessageInterceptor interceptor) {
		synchronized (CHANNELS_INTERCEPTORS) {
			CHANNELS_INTERCEPTORS.put(channelId, interceptor);
		}
	}

	public static void removeInterceptor(Snowflake channelId, MessageInterceptor interceptor) {
		synchronized (CHANNELS_INTERCEPTORS) {
			Collection<MessageInterceptor> listeners = CHANNELS_INTERCEPTORS.get(channelId);
			listeners.remove(interceptor);
			if(listeners.isEmpty()) {
				CHANNELS_INTERCEPTORS.removeAll(channelId);
			}
		}
	}

	/**
	 * @param event - the event to intercept
	 * @return true if the event has been intercepted (blocking the execution of other commands), false otherwise
	 */
	public static Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		synchronized (CHANNELS_INTERCEPTORS) {
			return Flux.fromIterable(CHANNELS_INTERCEPTORS.get(event.getMessage().getChannelId()))
					.flatMap(interceptor -> interceptor.isIntercepted(event))
					.filter(Boolean.TRUE::equals)
					.hasElements();
		}
	}
}
