package me.shadorc.shadbot.core.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.SchedulerUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public abstract class AbstractGameManager {

	private final Context context;
	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(Context context) {
		this.context = context;
	}

	// FIXME: Throws Exception ?
	public abstract void start() throws Exception;

	public abstract void stop();

	public Context getContext() {
		return context;
	}

	/**
	 * @param message - the message
	 * @return A {@link Mono} that indicates whether the message is a valid cancel command or not
	 */
	public final Mono<Boolean> isCancelCmd(Message message) {
		return Mono.just(message)
				// This is not a webhook nor an embed
				.filter(msg -> msg.getAuthorId().isPresent() && msg.getContent().isPresent())
				.map(msg -> msg.getContent().get())
				// This is a cancel command
				.filter(content -> content.equals(context.getPrefix() + "cancel"))
				.flatMap(content -> message.getAuthorAsMember())
				.zipWith(DiscordUtils.hasPermissions(message.getAuthorAsMember(), Permission.ADMINISTRATOR))
				// The author is the author of the game or he is an administrator
				.map(memberAndIsAdmin -> context.getAuthorId().equals(memberAndIsAdmin.getT1().getId()) || memberAndIsAdmin.getT2())
				.defaultIfEmpty(false)
				// Send a message indicating that the game has been cancelled
				.doOnSuccess(isCancel -> {
					if(isCancel) {
						message.getAuthor()
								.map(User::getUsername)
								.flatMap(username -> BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", username), message.getChannel()))
								.subscribe();
					}
				});
	}

	public boolean isTaskDone() {
		return scheduledTask == null || scheduledTask.isDone();
	}

	public void schedule(Runnable command, long delay, TimeUnit unit) {
		this.cancelScheduledTask();
		scheduledTask = SchedulerUtils.schedule(command, delay, unit);
	}

	public void cancelScheduledTask() {
		if(!this.isTaskDone()) {
			scheduledTask.cancel(false);
		}
	}

}
