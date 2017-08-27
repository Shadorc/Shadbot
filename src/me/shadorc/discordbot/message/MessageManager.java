package me.shadorc.discordbot.message;

import java.util.concurrent.ConcurrentHashMap;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private final static ConcurrentHashMap<IChannel, MessageListener> CHANNELS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(IChannel channel, MessageListener listener) {
		CHANNELS_LISTENERS.put(channel, listener);
	}

	public static void removeListener(IChannel channel) {
		CHANNELS_LISTENERS.remove(channel);
	}

	public static boolean isWaitingForMessage(IChannel channel) {
		return CHANNELS_LISTENERS.containsKey(channel);
	}

	public static boolean notify(IMessage message) {
		boolean isBlocking = false;
		for(IChannel channel : CHANNELS_LISTENERS.keySet()) {
			isBlocking = CHANNELS_LISTENERS.get(channel).onMessageReceived(message);
		}
		return isBlocking;
	}
}
