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
	// Used to ensure the veracity of isTaskDone, certain conditions may lead to the task
	// being completed without the Mono being disposed or null leading to isTaskDone returning false
	private final AtomicBoolean isDone;
	private Disposable scheduledTask;

	public GameManager(GameCmd<?> gameCmd, Context context) {
		this.gameCmd = gameCmd;
		this.context = context;
		this.isDone = new AtomicBoolean(false);
	}

	public abstract void start();

	public final void stop() {
		this.cancelScheduledTask();
		MessageInterceptorManager.removeInterceptor(this.getContext().getChannelId(), this);
		this.gameCmd.getManagers().remove(this.getContext().getChannelId());
	}

	public abstract Mono<Void> show();

	public Context getContext() {
		return this.context;
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

	public <T> void schedule(Mono<T> mono, Duration duration) {
		this.cancelScheduledTask();
		this.scheduledTask = Mono.delay(duration)
				.doOnNext(ignored -> this.isDone.set(true))
				.then(mono)
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err));
	}

	public void cancelScheduledTask() {
		if(this.scheduledTask != null) {
			this.scheduledTask.dispose();
		}
	}

}
