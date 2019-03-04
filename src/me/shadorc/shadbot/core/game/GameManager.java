package me.shadorc.shadbot.core.game;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public abstract class GameManager implements MessageInterceptor {

	private final GameCmd<?> gameCmd;
	private final Context context;
	private final Duration duration;
	private final AtomicBoolean isScheduled;
	private Disposable scheduledTask;

	public GameManager(GameCmd<?> gameCmd, Context context, Duration duration) {
		this.gameCmd = gameCmd;
		this.context = context;
		this.duration = duration;
		this.isScheduled = new AtomicBoolean(false);
	}

	public abstract void start();

	public final void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		this.gameCmd.getManagers().remove(this.getContext().getChannelId());
	}

	public abstract Mono<Void> show();

	/**
	 * Schedule a {@link Mono} that will be triggered when the game duration is elapsed.
	 * 
	 * @param mono - The {@link Mono} to trigger after the game duration has elapsed.
	 */
	public <T> void schedule(Mono<T> mono) {
		this.cancelScheduledTask();
		this.isScheduled.set(true);
		this.scheduledTask = Mono.delay(this.getDuration())
				.doOnNext(ignored -> this.isScheduled.set(false))
				.then(mono)
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err));
	}

	/**
	 * Cancel the current task, if scheduled.
	 */
	public void cancelScheduledTask() {
		this.isScheduled.set(false);
		if(this.scheduledTask != null) {
			this.scheduledTask.dispose();
		}
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

	/**
	 * @param message - the {@link Message} to check
	 * @return A {@link Mono} that returns true if the {@link Message} is a valid cancel command, false otherwise
	 */
	private Mono<Boolean> isCancelMessage(Message message) {
		return Mono.just(message)
				.filter(msg -> msg.getAuthor().isPresent() && msg.getContent().isPresent())
				.filter(msg -> msg.getContent().get().equals(String.format("%scancel", this.context.getPrefix())))
				.flatMap(msg -> msg.getChannel()
						.flatMap(channel -> DiscordUtils.hasPermission(channel, message.getAuthor().get().getId(), Permission.ADMINISTRATOR))
						// The author is the author of the game or he is an administrator
						.map(isAdmin -> this.context.getAuthorId().equals(message.getAuthor().get().getId()) || isAdmin))
				.defaultIfEmpty(false);
	}

	/**
	 * @return {@code true} if a task is currently scheduled, {@code false} otherwise.
	 */
	public boolean isScheduled() {
		return this.isScheduled.get();
	}

	public Context getContext() {
		return this.context;
	}

	public Duration getDuration() {
		return this.duration;
	}

}
