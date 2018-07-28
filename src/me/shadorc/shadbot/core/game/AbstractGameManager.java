package me.shadorc.shadbot.core.game;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import reactor.core.publisher.Mono;

public abstract class AbstractGameManager {

	private final Context context;
	private ScheduledFuture<?> scheduledTask;

	public AbstractGameManager(Context context) {
		this.context = context;
	}

	public abstract Mono<Void> start();

	public abstract Mono<Void> stop();

	public Context getContext() {
		return context;
	}

	/**
	 * @param message - the {@link Message} to check
	 * @return A {@link Mono} that returns true if the {@link Message} is a valid cancel command, false otherwise
	 */
	private Mono<Boolean> isCancelCmd(Message message) {
		return Mono.just(message)
				// This is not a webhook nor an embed
				.filter(msg -> msg.getAuthorId().isPresent() && msg.getContent().isPresent())
				.map(msg -> msg.getContent().get())
				// This is a cancel command
				.filter(content -> content.equals(context.getPrefix() + "cancel"))
				.flatMap(content -> message.getAuthorAsMember())
				.zipWith(DiscordUtils.hasPermissions(message.getAuthorAsMember(), Permission.ADMINISTRATOR))
				// The author is the author of the game or he is an administrator
				.filter(memberAndIsAdmin -> context.getAuthorId().equals(memberAndIsAdmin.getT1().getId()) || memberAndIsAdmin.getT2())
				.hasElement();
	}

	/**
	 * @param message - the {@link Message} to check
	 * @param mono - the {@link Mono} to execute if the {@link Message} is not a cancel command
	 * @return A {@link Mono} that returns true if the message is intercepted, false otherwise
	 */
	public Mono<Boolean> cancelOrDo(Message message, Mono<Boolean> mono) {
		return Mono.just(message)
				.filterWhen(msg -> this.isCancelCmd(msg))
				.flatMap(msg -> msg.getAuthor())
				.map(User::getUsername)
				.flatMap(username -> BotUtils.sendMessage(
						String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", username), message.getChannel()))
				.flatMap(ignored -> this.stop())
				// The message is intercepted, return true
				.map(ignored -> Boolean.TRUE)
				.switchIfEmpty(mono);
	}

	public boolean isTaskDone() {
		return scheduledTask == null || scheduledTask.isDone();
	}

	public void schedule(Runnable command, long delay, TimeUnit unit) {
		this.cancelScheduledTask();
		scheduledTask = Shadbot.getScheduler().schedule(command, delay, unit);
	}

	public void cancelScheduledTask() {
		if(scheduledTask != null) {
			scheduledTask.cancel(false);
		}
	}

}
