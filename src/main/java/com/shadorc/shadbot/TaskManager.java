package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.cache.GuildOwnersCache;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.users.entity.achievement.Achievement;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ProcessUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import io.prometheus.client.Gauge;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void schedulePresenceUpdates() {
        this.logger.info("Scheduling presence updates");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofMinutes(15), this.defaultScheduler)
                .map(ignored -> DiscordUtils.getRandomStatus())
                .flatMap(this.gateway::updatePresence)
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void scheduleLottery() {
        this.logger.info("Starting lottery (next draw in {})", FormatUtils.customDate(LotteryCmd.getDelay()));
        final Disposable task = Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), this.defaultScheduler)
                .flatMap(ignored -> LotteryCmd.draw(this.gateway))
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void schedulePeriodicStats() {
        this.logger.info("Scheduling periodic stats log");

        final Gauge ramUsageGauge = Gauge.build().namespace("process").name("ram_usage_mb")
                .help("Ram usage in MB").register();
        final Gauge cpuUsageGauge = Gauge.build().namespace("process").name("cpu_usage_percent")
                .help("CPU usage in percent").register();
        final Gauge threadCountGauge = Gauge.build().namespace("process").name("thread_count")
                .help("Thread count").register();
        final Gauge gcCountGauge = Gauge.build().namespace("process").name("gc_count")
                .help("Garbage collector count").register();
        final Gauge gcTimeGauge = Gauge.build().namespace("process").name("gc_time")
                .help("Garbage collector total time in ms").register();
        final Gauge responseTimeGauge = Gauge.build().namespace("shard").name("response_time")
                .help("Shard response time").labelNames("shard_id").register();
        final Gauge guildCountGauge = Gauge.build().namespace("shadbot")
                .name("guild_count").help("Guild count").register();

        final GatewayClientGroup group = this.gateway.getGatewayClientGroup();
        final Mono<Map<Integer, Long>> getResponseTimes = Flux.range(0, group.getShardCount())
                .flatMap(i -> Mono.justOrEmpty(group.find(i))
                        .map(GatewayClient::getResponseTime)
                        .map(Duration::toMillis)
                        .map(millis -> Tuples.of(i, millis)))
                .collectMap(Tuple2::getT1, Tuple2::getT2);

        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofSeconds(15), this.defaultScheduler)
                .doOnNext(ignored -> {
                    ramUsageGauge.set(ProcessUtils.getMemoryUsed());
                    cpuUsageGauge.set(ProcessUtils.getCpuUsage());
                    threadCountGauge.set(Thread.activeCount());
                    gcCountGauge.set(ProcessUtils.getGCCount());
                    gcTimeGauge.set(ProcessUtils.getGCTime());
                    guildCountGauge.set(GuildOwnersCache.count());
                })
                .flatMap(ignored -> getResponseTimes)
                .doOnNext(responseTimeMap -> responseTimeMap
                        .forEach((key, value) -> responseTimeGauge.labels(key.toString()).set(value)))
                .subscribe(null, ExceptionHandler::handleUnknownError);

        this.tasks.add(task);
    }

    public void schedulePostStats() {
        this.logger.info("Starting bot list stats scheduler");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofHours(3), this.defaultScheduler)
                .flatMap(ignored -> this.botListStats.postStats())
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void scheduleVotersCheck() {
        this.logger.info("Starting voters checker scheduler");
        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofMinutes(30), this.defaultScheduler)
                .flatMap(ignored -> this.botListStats.getStats())
                .flatMap(DatabaseManager.getUsers()::getDBUser)
                .flatMap(dbUser -> dbUser.unlockAchievement(Achievement.VOTER))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}
