package com.shadorc.shadbot.core.game;

import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.object.ExceptionHandler;
import discord4j.core.object.entity.Message;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Game {

    protected final Context context;
    protected final Duration duration;
    protected final AtomicBoolean isScheduled;
    protected final List<GameListener> listeners;

    protected Disposable scheduledTask;

    protected Game(Context context, Duration duration) {
        this.context = context;
        this.duration = duration;
        this.listeners = new ArrayList<>(1);
        this.isScheduled = new AtomicBoolean(false);
    }

    public abstract Mono<Void> start();

    public abstract Mono<Message> show();

    public abstract Mono<Void> end();

    public void addGameListener(GameListener listener) {
        this.listeners.add(listener);
    }

    public void destroy() {
        this.cancelScheduledTask();
        this.listeners.forEach(listener -> listener.onGameDestroy(this.context.getChannelId()));
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

    public Context getContext() {
        return this.context;
    }

    public Duration getDuration() {
        return this.duration;
    }

}
