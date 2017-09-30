package me.shadorc.discordbot.utils.schedule;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.schedule.ScheduledMessage.Reason;
import sx.blah.discord.handle.obj.IChannel;

public class Scheduler {

	protected static final ConcurrentHashMap<IChannel, List<ScheduledMessage>> MESSAGE_QUEUE = new ConcurrentHashMap<>();

	public static void start() {
		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(() -> NetUtils.postStats(), 0, TimeUnit.HOURS.toMillis(3), TimeUnit.MILLISECONDS);

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(() -> Storage.save(), TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS);

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(() -> Stats.save(), TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS);
	}

	public static void scheduleMessages(Object message, IChannel channel, Reason reason) {
		MESSAGE_QUEUE.putIfAbsent(channel, new ArrayList<>());

		List<ScheduledMessage> channelQueue = MESSAGE_QUEUE.get(channel);
		ScheduledMessage scheduledMsg = new ScheduledMessage(message, channel, reason);

		if(channelQueue.contains(scheduledMsg)) {
			return;
		}

		channelQueue.add(scheduledMsg);

		// Wait for shard to be ready to send them
		if(reason.equals(Reason.SHARD_NOT_READY)) {
			return;
		}

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.submit(() -> {
			Scheduler.waitAndSend(scheduledMsg);
			MESSAGE_QUEUE.get(channel).remove(message);
			if(MESSAGE_QUEUE.get(channel).isEmpty()) {
				MESSAGE_QUEUE.remove(channel);
			}
			executor.shutdown();
		});
	}

	public static void sendMsgWaitingForShard() {
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
		executor.submit(() -> {
			Iterator<IChannel> channelItr = MESSAGE_QUEUE.keySet().iterator();
			while(channelItr.hasNext()) {
				IChannel channel = channelItr.next();
				Iterator<ScheduledMessage> msgItr = MESSAGE_QUEUE.get(channel).iterator();

				while(msgItr.hasNext()) {
					ScheduledMessage message = msgItr.next();
					if(message.getReason().equals(Reason.SHARD_NOT_READY)) {
						Scheduler.waitAndSend(message);
						msgItr.remove();
					}
				}

				if(MESSAGE_QUEUE.get(channel).isEmpty()) {
					channelItr.remove();
				}
			}
			executor.shutdown();
		});
	}

	protected static void waitAndSend(ScheduledMessage message) {
		int count = 0;
		boolean success = false;
		while(!success && count < 3) {
			Utils.sleep(TimeUnit.SECONDS.toMillis(Config.DEFAULT_RETRY_TIME));
			LogUtils.warn("{Guild ID: " + message.getChannel().getGuild().getLongID() + "} Sending pending message...");
			success = message.send() != null;
			count++;
		}

		if(success) {
			LogUtils.warn("{Guild ID: " + message.getChannel().getGuild().getLongID() + "} Pending message sent.");
		} else {
			LogUtils.warn("{Guild ID: " + message.getChannel().getGuild().getLongID() + "} Too many try, abort attempt to send message.");
		}
	}

	public static void forceExecution() {
		try {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.submit(() -> {
				Storage.save();
				Stats.save();
				executor.shutdown();
			}).get();
		} catch (InterruptedException | ExecutionException err) {
			LogUtils.error("An error occured while forcing saves.", err);
		}
	}
}
