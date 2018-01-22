package me.shadorc.shadbot.core.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.executor.ShadbotScheduledExecutor;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

public abstract class AbstractGameManager {

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ShadbotScheduledExecutor("Shadbot-GameManager-%d");

	private final String cmdName;
	private final String prefix;
	private final IChannel channel;
	private final IUser author;

	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(AbstractCommand cmd, String prefix, IChannel channel, IUser author) {
		this.cmdName = cmd.getName();
		this.prefix = prefix;
		this.channel = channel;
		this.author = author;
	}

	@SuppressWarnings("PMD.SignatureDeclareThrowsException")
	public abstract void start() throws Exception;

	public abstract void stop();

	public final boolean isCancelCmd(IMessage message) {
		IUser user = message.getAuthor();
		if(message.getContent().equals(this.getPrefix() + "cancel")
				&& (author.equals(user) || PermissionUtils.hasPermissions(channel, user, Permissions.ADMINISTRATOR))) {
			BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", user.getName()), channel);
			this.stop();
			return true;
		}
		return false;
	}

	public String getCmdName() {
		return cmdName;
	}

	public String getPrefix() {
		return prefix;
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

	public final void schedule(Runnable command, long delay, TimeUnit unit) {
		if(scheduledTask != null) {
			this.cancelScheduledTask();
		}
		scheduledTask = SCHEDULED_EXECUTOR.schedule(command, delay, unit);
	}

	public final void cancelScheduledTask() {
		scheduledTask.cancel(false);
	}

}
