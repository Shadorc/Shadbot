package me.shadorc.discordbot;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.shadorc.discordbot.data.Stats;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;

public class SchedulerManager {

	private static Runnable saveStatsTask;
	private static Runnable postStatsTask;

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

	public static void forceExecution() {
		try {
			Executors.newSingleThreadScheduledExecutor().submit(saveStatsTask).get();
		} catch (InterruptedException | ExecutionException err) {
			LogUtils.error("An error occured while forcing stats saving.", err);
		}
	}

}
