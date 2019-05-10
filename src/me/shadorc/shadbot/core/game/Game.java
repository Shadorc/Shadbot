package me.shadorc.shadbot.core.game;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Permission;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Game {

    private final GameCmd<?> gameCmd;
    private final Context context;
    private final Duration duration;
    private final AtomicBoolean isScheduled;
    private Disposable scheduledTask;

    public Game(GameCmd<?> gameCmd, Context context, Duration duration) {
        this.gameCmd = gameCmd;
        this.context = context;
        this.duration = duration;
        this.isScheduled = new AtomicBoolean(false);
    }

    public abstract void start();

    public abstract Mono<Void> end();

    public final void stop() {
        this.cancelScheduledTask();
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
        this.scheduledTask = Mono.delay(this.getDuration(), Schedulers.elastic())
                .doOnNext(ignored -> this.isScheduled.set(false))
                .then(mono)
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.getContext().getClient(), err));
    }

    /**
     * Cancel the current task, if scheduled.
     */
    private void cancelScheduledTask() {
        if (this.isScheduled()) {
            this.scheduledTask.dispose();
            this.isScheduled.set(false);
        }
    }

    /**
     * @param message - the {@link Message} to check
     * @return A {@link Mono} that returns {@code true} if the {@link Message} is a valid
     * cancel command, {@code false} otherwise.
     */
    public Mono<Boolean> isCancelMessage(Message message) {
        if (message.getContent().isEmpty() || message.getAuthor().isEmpty()) {
            return Mono.just(false);
        }

        final String content = message.getContent().get();
        final User author = message.getAuthor().get();
        if (content.equals(String.format("%scancel", this.context.getPrefix()))) {
            return message.getChannel()
                    .flatMap(channel -> DiscordUtils.hasPermission(channel, author.getId(), Permission.ADMINISTRATOR))
                    // The author is the author of the game or he is an administrator
                    .map(isAdmin -> this.context.getAuthorId().equals(author.getId()) || isAdmin);
        }

        return Mono.just(false);
    }

    /**
     * @return {@code true} if a task is currently scheduled, {@code false} otherwise.
     */
    public boolean isScheduled() {
        return this.scheduledTask != null && this.isScheduled.get();
    }

    public Context getContext() {
        return this.context;
    }

    public Duration getDuration() {
        return this.duration;
    }

}
