package me.shadorc.shadbot.utils;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;

public class SchedulerUtils {

	private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledWrappedExecutor(3, "ShadbotScheduler-%d");

	public static ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return SCHEDULER.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

	public static ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
		return SCHEDULER.schedule(command, delay, unit);
	}

}
