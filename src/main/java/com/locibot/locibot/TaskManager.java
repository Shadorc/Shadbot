package com.locibot.locibot;

import com.locibot.locibot.command.game.lottery.LotteryCmd;
import com.locibot.locibot.api.BotListStats;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LogUtil;
import com.locibot.locibot.utils.SystemUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class TaskManager {

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final Logger LOGGER = LogUtil.getLogger(TaskManager.class);

    private final List<Disposable> tasks;

    public TaskManager() {
        this.tasks = new ArrayList<>(4);
    }

    public void scheduleLottery(GatewayDiscordClient gateway) {
        LOGGER.info("Starting lottery (next draw in {})",
                FormatUtil.formatDurationWords(Config.DEFAULT_LOCALE, LotteryCmd.getDelay()));
        final Disposable task = Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), DEFAULT_SCHEDULER)
                .flatMap(__ -> LotteryCmd.draw(gateway)
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void schedulePeriodicStats(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling periodic telemetry statistics post");

        final GatewayClientGroup group = gateway.getGatewayClientGroup();
        final Disposable task = Flux.interval(Duration.ZERO, Duration.ofSeconds(15), DEFAULT_SCHEDULER)
                .doOnNext(__ -> {
                    LOGGER.debug("Updating telemetry statistics");
                    Telemetry.UPTIME_GAUGE.set(SystemUtil.getUptime().toMillis());
                    Telemetry.PROCESS_CPU_USAGE_GAUGE.set(SystemUtil.getProcessCpuUsage());
                    Telemetry.SYSTEM_CPU_USAGE_GAUGE.set(SystemUtil.getSystemCpuUsage());
                    Telemetry.MAX_HEAP_MEMORY_GAUGE.set(SystemUtil.getMaxHeapMemory());
                    Telemetry.TOTAL_HEAP_MEMORY_GAUGE.set(SystemUtil.getTotalHeapMemory());
                    Telemetry.USED_HEAP_MEMORY_GAUGE.set(SystemUtil.getUsedHeapMemory());
                    Telemetry.SYSTEM_TOTAL_MEMORY_GAUGE.set(SystemUtil.getSystemTotalMemory());
                    Telemetry.SYSTEM_FREE_MEMORY_GAUGE.set(SystemUtil.getSystemFreeMemory());
                    Telemetry.GC_COUNT_GAUGE.set(SystemUtil.getGCCount());
                    Telemetry.GC_TIME_GAUGE.set(SystemUtil.getGCTime().toMillis());
                    Telemetry.THREAD_COUNT_GAUGE.set(SystemUtil.getThreadCount());
                    Telemetry.DAEMON_THREAD_COUNT_GAUGE.set(SystemUtil.getDaemonThreadCount());

                    Telemetry.PROCESS_TOTAL_MEMORY.set(SystemUtil.getProcessTotalMemory());
                    Telemetry.PROCESS_FREE_MEMORY.set(SystemUtil.getProcessFreeMemory());
                    Telemetry.PROCESS_MAX_MEMORY.set(SystemUtil.getProcessMaxMemory());

                    Telemetry.GUILD_COUNT_GAUGE.set(Telemetry.GUILD_IDS.size());
                    Telemetry.UNIQUE_INTERACTING_USERS.set(Telemetry.INTERACTING_USERS.size());

                    for (int i = 0; i < group.getShardCount(); ++i) {
                        final long responseTime = group.find(i)
                                .map(GatewayClient::getResponseTime)
                                .map(Duration::toMillis)
                                .orElse(0L);
                        Telemetry.RESPONSE_TIME_GAUGE.labels(Integer.toString(i)).set(responseTime);
                    }
                    LOGGER.debug("Telemetry statistics updated");
                })
                .subscribe(null, ExceptionHandler::handleUnknownError);

        this.tasks.add(task);
    }

    public void schedulePostStats(BotListStats botListStats) {
        LOGGER.info("Starting bot list stats scheduler");
        final Disposable task = Flux.interval(Duration.ofMinutes(15), Duration.ofHours(3), DEFAULT_SCHEDULER)
                .flatMap(__ -> botListStats.postStats())
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}