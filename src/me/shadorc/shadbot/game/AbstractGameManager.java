package me.shadorc.shadbot.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.ThreadPoolUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;

public abstract class AbstractGameManager {

	private static final ScheduledThreadPoolExecutor SCHEDULER =
			ThreadPoolUtils.newSingleScheduledThreadPoolExecutor("Shadbot-GameManager-%d");

	private final String cmdName;
	private final IChannel channel;
	private final IUser author;

	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(AbstractCommand cmd, IChannel channel, IUser author) {
		this.cmdName = cmd.getName();
		this.channel = channel;
		this.author = author;
	}

	@SuppressWarnings("PMD.SignatureDeclareThrowsException")
	public abstract void start() throws Exception;

	public abstract void stop();

	public String getCmdName() {
		return cmdName;
	}

	public IGuild getGuild() {
		return channel.getGuild();
	}

	public IChannel getChannel() {
		return channel;
	}

	public IUser getAuthor() {
		return author;
	}

	public boolean isTaskDone() {
		return scheduledTask == null || scheduledTask.isDone();
	}

	public void schedule(Runnable command, long delay, TimeUnit unit) {
		if(scheduledTask != null) {
			this.cancelScheduledTask();
		}
		scheduledTask = SCHEDULER.schedule(command, delay, unit);
	}

	public void cancelScheduledTask() {
		scheduledTask.cancel(true);
	}

}
