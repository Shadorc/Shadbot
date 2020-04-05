package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.owner.ResourceStatsCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private final GatewayDiscordClient gateway;
    private final Scheduler defaultScheduler;
    private final Logger logger;
    private final BotListStats botListStats;
    private final List<Disposable> tasks;

    public TaskManager(GatewayDiscordClient gateway) {
        this.gateway = gateway;
        this.defaultScheduler = Schedulers.boundedElastic();
        this.logger = Loggers.getLogger("shadbot.taskmanager");
        this.botListStats = new BotListStats(gateway);
        this.tasks = new ArrayList<>(4);
    }

    public void schedulesPresenceUpdates() {
        this.logger.info("Scheduling presence updates...");
        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofMinutes(30), this.defaultScheduler)
                .flatMap(ignored -> {
                    final String presence = String.format("%shelp | %s", Config.DEFAULT_PREFIX,
                            TextUtils.TIPS.getRandomTextFormatted());
                    return this.gateway.updatePresence(Presence.online(Activity.playing(presence)));
                })
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void schedulesLottery() {
        this.logger.info("Starting lottery... Next lottery draw in {}", LotteryCmd.getDelay());
        final Disposable task = Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), this.defaultScheduler)
                .flatMap(ignored -> LotteryCmd.draw(this.gateway))
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void schedulesSystemResourcesLog() {
        this.logger.info("Scheduling system resources log...");
        final Disposable task = DatabaseManager.getStats().dropSystemStats()
                .thenMany(Flux.interval(Duration.ZERO, ResourceStatsCmd.UPDATE_INTERVAL, this.defaultScheduler)
                        .flatMap(ignored -> DatabaseManager.getStats().logSystemResources())
                        .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err)))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void schedulesPostStats() {
        this.logger.info("Starting bot list stats scheduler...");
        final Disposable task = Flux.interval(Duration.ofHours(3), Duration.ofHours(3), this.defaultScheduler)
                .flatMap(ignored -> this.botListStats.postStats())
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}
