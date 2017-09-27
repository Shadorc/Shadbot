package me.shadorc.discordbot.utils.schedule;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
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

	private static Runnable postStatsTask;
	private static Runnable saveDataTask;
	private static Runnable saveStatsTask;

	public static void start() {
		// Update Shadbot stats every 3 hours
		postStatsTask = new Runnable() {
			@Override
			public void run() {
				NetUtils.postStats();
			}
		};

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(postStatsTask, 0, TimeUnit.HOURS.toMillis(3), TimeUnit.MILLISECONDS);

		// Save data every minute
		saveDataTask = new Runnable() {
			@Override
			public void run() {
				Storage.save();
			}
		};

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(saveDataTask, TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS);

		// Save stats every 5 minutes
		saveStatsTask = new Runnable() {
			@Override
			public void run() {
				Stats.save();
			}
		};

		Executors.newSingleThreadScheduledExecutor()
				.scheduleAtFixedRate(saveStatsTask, TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), TimeUnit.MILLISECONDS);
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

		Executors.newSingleThreadScheduledExecutor().submit(() -> Scheduler.waitAndSend(scheduledMsg));
	}

	public static void sendMsgWaitingForShard() {
		Executors.newSingleThreadScheduledExecutor().submit(() -> MESSAGE_QUEUE.keySet().stream()
				.forEach(channel -> MESSAGE_QUEUE.get(channel).stream()
						.filter(msg -> msg.getReason().equals(Reason.SHARD_NOT_READY))
						.forEach(msg -> Scheduler.waitAndSend(msg))));
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

		MESSAGE_QUEUE.get(message.getChannel()).remove(message);
		if(MESSAGE_QUEUE.get(message.getChannel()).isEmpty()) {
			MESSAGE_QUEUE.remove(message.getChannel());
		}
	}

	public static void forceExecution() {
		try {
			if(saveDataTask != null) {
				Executors.newSingleThreadScheduledExecutor().submit(saveDataTask).get();
			}
			if(saveStatsTask != null) {
				Executors.newSingleThreadScheduledExecutor().submit(saveStatsTask).get();
			}
		} catch (InterruptedException | ExecutionException err) {
			LogUtils.error("An error occured while forcing saves.", err);
		}
	}
}
