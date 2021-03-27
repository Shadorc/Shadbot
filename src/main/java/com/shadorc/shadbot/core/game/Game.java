package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.ExceptionHandler;
import discord4j.discordjson.json.MessageData;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Game {

    protected final GameCmd<?> gameCmd;
    protected final Context context;
    protected final Duration duration;
    protected final AtomicBoolean isScheduled;
    protected Disposable scheduledTask;

    protected Game(GameCmd<?> gameCmd, Context context, Duration duration) {
        this.gameCmd = gameCmd;
        this.context = context;
        this.duration = duration;
        this.isScheduled = new AtomicBoolean(false);
    }

    public abstract Mono<Void> start();

    public abstract Mono<MessageData> show();

    public abstract Mono<Void> end();

    public void destroy() {
        this.cancelScheduledTask();
        this.gameCmd.getManagers().remove(this.context.getChannelId());
    }

    /**
     * Schedule a {@link Mono} that will be triggered when the game duration is elapsed.
     *
     * @param mono The {@link Mono} to trigger after the game duration has elapsed.
     */
    protected <T> void schedule(Mono<T> mono) {
        this.cancelScheduledTask();
        this.isScheduled.set(true);
        this.scheduledTask = Mono.delay(this.duration, Schedulers.boundedElastic())
                .doOnNext(__ -> this.isScheduled.set(false))
                .then(mono)
                .subscribe(null, ExceptionHandler::handleUnknownError);
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
     * @return {@code true} if a task is currently scheduled, {@code false} otherwise.
     */
    public boolean isScheduled() {
        return this.scheduledTask != null && this.isScheduled.get();
    }

    public GameCmd<?> getGameCmd() {
        return this.gameCmd;
    }

    public Context getContext() {
        return this.context;
    }

    public Duration getDuration() {
        return this.duration;
    }

}
