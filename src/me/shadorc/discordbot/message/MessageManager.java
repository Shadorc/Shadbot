package me.shadorc.discordbot.message;

import java.util.concurrent.ConcurrentHashMap;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private final static ConcurrentHashMap<IGuild, MessageListener> GUILDS_LISTENERS = new ConcurrentHashMap<>();

	public static void addListener(IGuild guild, MessageListener listener) {
		GUILDS_LISTENERS.put(guild, listener);
	}

	public static void removeListener(IGuild guild) {
		GUILDS_LISTENERS.remove(guild);
	}

	public static void notify(IMessage message) {
		for(IGuild guild : GUILDS_LISTENERS.keySet()) {
			GUILDS_LISTENERS.get(guild).onMessageReceived(message);
		}
	}

	public static boolean isWaitingForMessage(IGuild guild) {
		return GUILDS_LISTENERS.containsKey(guild);
	}

}
