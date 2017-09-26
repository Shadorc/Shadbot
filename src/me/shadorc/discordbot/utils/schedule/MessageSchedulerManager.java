package me.shadorc.discordbot.utils.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.obj.IChannel;

public class MessageSchedulerManager {

	private static final ConcurrentHashMap<IChannel, List<DelayedMessage>> MESSAGE_QUEUE = new ConcurrentHashMap<>();
	private static final int MAX_COUNT = 3;

	public static void delay(IChannel channel, Object object) {
		MESSAGE_QUEUE.putIfAbsent(channel, new ArrayList<>());

		List<DelayedMessage> list = MESSAGE_QUEUE.get(channel);

		DelayedMessage delayedMessage = new DelayedMessage(object);
		if(list.contains(delayedMessage)) {
			MessageSchedulerManager.incrementOrRemove(list, delayedMessage);
			if(list.isEmpty()) {
				MESSAGE_QUEUE.remove(channel);
			}
		} else {
			list.add(delayedMessage);
		}
	}

	private static void incrementOrRemove(List<DelayedMessage> list, DelayedMessage message) {
		list.get(list.indexOf(message)).incrementCount();
		if(list.get(list.indexOf(message)).getCount() > MAX_COUNT) {
			list.remove(message);
			LogUtils.info("Message removed from queue because sending it failed too many times.");
		}
	}

	/**
	 * Send pending embeds/messages
	 */
	public static void sendQueue() {
		if(!MESSAGE_QUEUE.isEmpty()) {
			int count = MESSAGE_QUEUE.values().stream().mapToInt(list -> list.size()).sum();
			LogUtils.info("Sending " + count + " pending message(s)...");
			for(IChannel channel : MESSAGE_QUEUE.keySet()) {
				for(DelayedMessage message : MESSAGE_QUEUE.get(channel)) {
					if(message.getMessage() instanceof String && BotUtils.send((String) message.getMessage(), channel).get() != null
							|| message.getMessage() instanceof EmbedObject && BotUtils.send((EmbedObject) message.getMessage(), channel).get() != null) {
						MESSAGE_QUEUE.get(channel).remove(message);
					}
				}
			}
			LogUtils.info("Pending message(s) sent.");
			MESSAGE_QUEUE.clear();
		}
	}
}
