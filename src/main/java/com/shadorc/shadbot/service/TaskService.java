package com.shadorc.shadbot.service;

import com.shadorc.shadbot.service.task.BotListStatsTask;
import com.shadorc.shadbot.service.task.LotteryTask;
import com.shadorc.shadbot.service.task.Task;
import com.shadorc.shadbot.service.task.TelemetryTask;
import discord4j.core.GatewayDiscordClient;
import reactor.core.Disposable;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class TaskService implements Service {

    private final Scheduler scheduler;
    private final List<Task> tasks;
    private final List<Disposable> disposables;

    public TaskService(GatewayDiscordClient gateway) {
        this.scheduler = Schedulers.boundedElastic();
        this.tasks = List.of(
                new LotteryTask(gateway),
                new TelemetryTask(gateway),
                new BotListStatsTask(gateway));
        this.disposables = new ArrayList<>(this.tasks.size());
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void start() {
        this.disposables.addAll(this.tasks.stream()
                .filter(Task::isEnabled)
                .map(task -> task.schedule(this.scheduler))
                .toList());
    }

    @Override
    public void stop() {
        this.disposables.forEach(Disposable::dispose);
    }
}
