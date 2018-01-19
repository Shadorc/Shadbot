package me.shadorc.shadbot.message;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private static final ConcurrentHashMap<Long, List<MessageListener>> CHANNELS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(IChannel channel, MessageListener listener) {
		CHANNELS_LISTENERS.putIfAbsent(channel.getLongID(), new CopyOnWriteArrayList<>(new ArrayList<>()));
		CHANNELS_LISTENERS.get(channel.getLongID()).add(listener);
	}

	public static void removeListener(IChannel channel, MessageListener listener) {
		List<MessageListener> list = CHANNELS_LISTENERS.get(channel.getLongID());
		if(list != null) {
			list.remove(listener);
			if(list.isEmpty()) {
				CHANNELS_LISTENERS.remove(channel.getLongID());
			}
		}
	}

	/**
	 * @param message - the message received
	 * @return true if the message has been intercepted, false otherwise
	 */
	public static boolean intercept(IMessage message) {
		List<MessageListener> listeners = CHANNELS_LISTENERS.get(message.getChannel().getLongID());
		if(listeners == null) {
			return false;
		}
		return listeners.stream().map(listener -> listener.intercept(message)).anyMatch(Boolean.TRUE::equals);
	}
}
