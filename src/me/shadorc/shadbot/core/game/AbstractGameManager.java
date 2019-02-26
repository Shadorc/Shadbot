package me.shadorc.shadbot.core.game;

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public abstract class AbstractGameManager {

	private final Context context;
	/**
	 * Used to ensure the veracity of {@code isTaskDone}, certain conditions may lead to the task being completed without the Mono being disposed or null
	 * leading to {@code isTaskDone} returning false
	 */
	private final AtomicBoolean isDone;
	private Disposable scheduledTask;

	public AbstractGameManager(Context context) {
		this.context = context;
		this.isDone = new AtomicBoolean(false);
	}

	public abstract void start();

	public abstract void stop();

	public abstract Mono<Void> show();

	public Context getContext() {
		return this.context;
	}

	/**
	 * @param message - the {@link Message} to check
	 * @return A {@link Mono} that returns true if the {@link Message} is a valid cancel command, false otherwise
	 */
	private Mono<Boolean> isCancelMessage(Message message) {
		if(message.getAuthor().isEmpty() || message.getContent().isEmpty()) {
			return Mono.just(false);
		}

		final String content = message.getContent().get();
		if(!String.format("%scancel", this.context.getPrefix()).equals(content)) {
			return Mono.just(false);
		}

		final Snowflake authorId = message.getAuthor().get().getId();
		return message.getChannel()
				.flatMap(channel -> DiscordUtils.hasPermission(channel, authorId, Permission.ADMINISTRATOR))
				// The author is the author of the game or he is an administrator
				.map(isAdmin -> this.context.getAuthorId().equals(authorId) || isAdmin);
	}

	/**
	 * @param message - the {@link Message} to check
	 * @param mono - the {@link Mono} to execute if the {@link Message} is not a cancel command
	 * @return A {@link Mono} that returns true if the message is intercepted, false otherwise
	 */
	public Mono<Boolean> cancelOrDo(Message message, Mono<Boolean> mono) {
		return Mono.just(message)
				.filterWhen(this::isCancelMessage)
				.map(Message::getAuthor)
				.flatMap(Mono::justOrEmpty)
				.map(User::getUsername)
				.flatMap(username -> this.context.getChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.CHECK_MARK + " Game cancelled by **%s**.", username), channel)))
				.flatMap(ignored -> Mono.fromRunnable(this::stop))
				// The message is intercepted, return true
				.map(ignored -> Boolean.TRUE)
				.switchIfEmpty(mono);
	}

	public boolean isTaskDone() {
		return this.isDone.get() || this.scheduledTask == null || this.scheduledTask.isDisposed();
	}

	public <T> void schedule(Mono<T> mono, long delay, TemporalUnit unit) {
		this.cancelScheduledTask();
		this.scheduledTask = Mono.delay(Duration.of(delay, unit))
				.then(Mono.fromRunnable(() -> this.isDone.set(true)))
				.then(mono)
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err));
	}

	public void cancelScheduledTask() {
		if(this.scheduledTask != null) {
			this.scheduledTask.dispose();
		}
	}

}
