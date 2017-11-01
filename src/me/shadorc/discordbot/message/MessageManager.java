package me.shadorc.discordbot.message;

import java.util.concurrent.ConcurrentHashMap;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private final static ConcurrentHashMap<Long, MessageListener> CHANNELS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(IChannel channel, MessageListener listener) {
		CHANNELS_LISTENERS.put(channel.getLongID(), listener);
	}

	public static void removeListener(IChannel channel) {
		CHANNELS_LISTENERS.remove(channel.getLongID());
	}

	public static boolean isWaitingForMessage(IChannel channel) {
		return CHANNELS_LISTENERS.containsKey(channel.getLongID());
	}

	public static void notify(IMessage message) {
		for(Long channelID : CHANNELS_LISTENERS.keySet()) {
			MessageListener msgListener = CHANNELS_LISTENERS.get(channelID);
			if(msgListener != null) {
				msgListener.onMessageReceived(message);
			}
		}
	}
}
