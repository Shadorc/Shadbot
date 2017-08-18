package me.shadorc.discordbot.message;

import java.util.HashMap;
import java.util.Map;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;

public class MessageManager {

	private final static Map<IGuild, MessageListener> GUILDS_LISTENERS = new HashMap<>();

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
