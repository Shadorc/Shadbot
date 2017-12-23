package me.shadorc.shadbot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import me.shadorc.discordbot.utils.Utils;
import me.shadorc.shadbot.data.AbstractData;

public class Scheduler {

	private static final ScheduledExecutorService SCHEDULED_EXECUTOR =
			Executors.newScheduledThreadPool(2, Utils.getThreadFactoryNamed("Shadbot-Scheduler-%d"));

	public static void register(AbstractData data) {
		SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> data.save(), data.getInitialDelay(), data.getPeriod(), data.getUnit());
	}

}
