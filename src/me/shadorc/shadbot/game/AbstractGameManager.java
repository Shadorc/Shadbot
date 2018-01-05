package me.shadorc.shadbot.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.ThreadPoolUtils;

public abstract class AbstractGameManager {

	private static final ScheduledThreadPoolExecutor SCHEDULER =
			ThreadPoolUtils.newSingleScheduledThreadPoolExecutor("Shadbot-GameManager-%d");

	private final String cmdName;

	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(AbstractCommand cmd) {
		this.cmdName = cmd.getName();
	}

	@SuppressWarnings("PMD.SignatureDeclareThrowsException")
	public abstract void start() throws Exception;

	public abstract void stop();

	public String getCmdName() {
		return cmdName;
	}

	public void schedule(Runnable command, long delay, TimeUnit unit) {
		scheduledTask = SCHEDULER.schedule(command, delay, unit);
	}

	public void cancelScheduledTask() {
		scheduledTask.cancel(true);
	}

}
