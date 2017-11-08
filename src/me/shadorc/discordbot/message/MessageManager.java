package me.shadorc.discordbot.message;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private final static ConcurrentHashMap<Long, List<MessageListener>> CHANNELS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(IChannel channel, MessageListener listener) {
		CHANNELS_LISTENERS.putIfAbsent(channel.getLongID(), new CopyOnWriteArrayList<>(new ArrayList<>()));
		CHANNELS_LISTENERS.get(channel.getLongID()).add(listener);
	}

	public static void removeListener(IChannel channel, MessageListener listener) {
		CHANNELS_LISTENERS.get(channel.getLongID()).remove(listener);
		if(CHANNELS_LISTENERS.get(channel.getLongID()).isEmpty()) {
			CHANNELS_LISTENERS.remove(channel.getLongID());
		}
	}

	public static boolean isWaitingForMessage(IChannel channel) {
		return CHANNELS_LISTENERS.containsKey(channel.getLongID());
	}

	public static boolean notify(IMessage message) {
		boolean isBlocked = false;
		ListIterator<MessageListener> iter = CHANNELS_LISTENERS.get(message.getChannel().getLongID()).listIterator();
		while(iter.hasNext()) {
			if(iter.next().onMessageReceived(message)) {
				isBlocked = true;
			}
		}
		return isBlocked;
	}
}
