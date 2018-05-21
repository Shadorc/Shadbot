package me.shadorc.shadbot.core.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public abstract class AbstractGameManager {

	private static final ScheduledThreadPoolExecutor SCHEDULED_EXECUTOR = new ScheduledWrappedExecutor("GameManager-%d");

	private final String cmdName;
	private final String prefix;
	private final Snowflake guildId;
	private final Snowflake channelId;
	private final Snowflake userId;

	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(AbstractCommand cmd, String prefix, Snowflake guildId, Snowflake channelId, Snowflake userId) {
		this.cmdName = cmd.getName();
		this.prefix = prefix;
		this.guildId = guildId;
		this.channelId = channelId;
		this.userId = userId;
	}

	public abstract void start() throws Exception;

	public abstract void stop();

	public final Mono<Boolean> isCancelCmd(Message message) {
		if(!message.getContent().isPresent() || !message.getAuthorId().isPresent()) {
			return Mono.just(false);
		}

		Mono<Boolean> isAdminMono = message.getAuthorAsMember().flatMapMany(Member::getRoles)
				.flatMapIterable(Role::getPermissions)
				.any(Permission.ADMINISTRATOR::equals);

		return message.getAuthorAsMember()
				.zipWith(isAdminMono)
				.flatMap(authorAndIsAdmin -> {
					Member author = authorAndIsAdmin.getT1();
					Boolean isAdmin = authorAndIsAdmin.getT2();
					String content = message.getContent().get();
					if(content.equals(prefix + "cancel") && (userId.equals(author.getId()) || isAdmin)) {
						BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", author.getUsername()), message.getChannel());
						this.stop();
						return Mono.just(true);
					}
					return Mono.just(false);
				});
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
		return userId;
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
