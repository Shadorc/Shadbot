package me.shadorc.discordbot.utils.task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.schedule.MessageSchedulerManager;

public class SchedulerManager {

	private static ScheduledFuture<?> sendMessagesSch;
	private static Runnable sendMessagesTask;

	private static Runnable postStatsTask;
	private static Runnable saveDataTask;
	private static Runnable saveStatsTask;

	public static void start() {
		sendMessagesTask = new Runnable() {
			@Override
			public void run() {
				MessageSchedulerManager.sendQueue();
			}
		};

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

	public static synchronized void scheduleSendingMessages() {
		if(sendMessagesSch == null || sendMessagesSch.isDone()) {
			sendMessagesSch = Executors.newSingleThreadScheduledExecutor()
					.schedule(sendMessagesTask, TimeUnit.SECONDS.toMillis(3), TimeUnit.MILLISECONDS);
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
