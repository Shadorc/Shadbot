package me.shadorc.shadbot.core.game;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public abstract class AbstractGameManager {

	private final Context context;
	/**
	 * Used to ensure the veracity of {@code isTaskDone}, certain conditions may lead to the task being completed without the
	 * Mono being disposed or null leading to {@code isTaskDone} returning false
	 */
	private final AtomicBoolean isDone;
	private Disposable scheduledTask;

	public AbstractGameManager(Context context) {
		this.context = context;
		this.isDone = new AtomicBoolean(false);
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
		return isDone.get() || scheduledTask == null || scheduledTask.isDisposed();
	}

	public void schedule(Mono<?> mono, long delay, TemporalUnit unit) {
		this.cancelScheduledTask();
		scheduledTask = Mono.delay(Duration.of(delay, unit))
				.then(Mono.fromRunnable(() -> this.isDone.set(true)))
				.then(mono)
				.subscribe();
	}

	public void cancelScheduledTask() {
		if(scheduledTask != null) {
			scheduledTask.dispose();
		}
	}

}
