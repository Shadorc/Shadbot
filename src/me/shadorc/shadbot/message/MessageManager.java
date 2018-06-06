package me.shadorc.shadbot.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;

public class MessageManager {

	/**
	 * TODO: Rework entirely this to work with temporary listeners
	 */

	private static final ConcurrentHashMap<Snowflake, List<MessageListener>> CHANNELS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(Snowflake channelId, MessageListener listener) {
		CHANNELS_LISTENERS.putIfAbsent(channelId, new CopyOnWriteArrayList<>(new ArrayList<>()));
		CHANNELS_LISTENERS.get(channelId).add(listener);
	}

	public static void removeListener(Snowflake channelId, MessageListener listener) {
		List<MessageListener> list = CHANNELS_LISTENERS.get(channelId);
		if(list != null) {
			list.remove(listener);
			if(list.isEmpty()) {
				CHANNELS_LISTENERS.remove(channelId);
			}
		}
	}

	/**
	 * @param guildId - the guild ID
	 * @param message - the message received
	 * @return true if the message has been intercepted, false otherwise
	 */
	public static boolean intercept(Snowflake guildId, Message message) {
		List<MessageListener> listeners = CHANNELS_LISTENERS.get(message.getChannelId());
		if(listeners == null) {
			return false;
		}
		return listeners.stream().map(listener -> listener.intercept(guildId, message))
				.anyMatch(Boolean.TRUE::equals);
	}
}
