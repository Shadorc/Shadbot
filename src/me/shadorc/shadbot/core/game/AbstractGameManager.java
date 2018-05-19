package me.shadorc.shadbot.core.game;

import java.security.Permissions;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;
import me.shadorc.shadbot.utils.object.Emoji;

public abstract class AbstractGameManager {

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ScheduledWrappedExecutor("GameManager-%d");

	private final String cmdName;
	private final String prefix;
	private final Snowflake guildId;
	private final Snowflake channelId;
	private final Snowflake memberId;

	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(AbstractCommand cmd, String prefix, TextChannel channel, Member member) {
		this.cmdName = cmd.getName();
		this.prefix = prefix;
		this.guildId = channel.getGuildId();
		this.channelId = channel.getId();
		this.memberId = member.getId();
	}

	public abstract void start() throws Exception;

	public abstract void stop();

	public final boolean isCancelCmd(Message message) {
		User user = message.getAuthor().block();
		if(message.getContent().equals(this.getPrefix() + "cancel")
				&& (memberId.equals(user.getId()) || PermissionUtils.hasPermissions(channel, user, Permissions.ADMINISTRATOR))) {
			BotUtils.sendMessage(
					String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", user.getUsername()), 
					message.getClient().getMessageChannelById(channelId).block());
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

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getChannelId() {
		return channelId;
	}

	public Snowflake getAuthorId() {
		return memberId;
	}

	public boolean isTaskDone() {
		return scheduledTask == null || scheduledTask.isDone();
	}

	public final void schedule(Runnable command, long delay, TimeUnit unit) {
		this.cancelScheduledTask();
		scheduledTask = SCHEDULED_EXECUTOR.schedule(command, delay, unit);
	}

	public final void cancelScheduledTask() {
		if(scheduledTask != null) {
			scheduledTask.cancel(false);
		}
	}

}
