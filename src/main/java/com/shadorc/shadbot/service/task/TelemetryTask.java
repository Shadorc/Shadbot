package com.shadorc.shadbot.service.task;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.SystemUtil;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.GatewayClientGroup;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.Duration;

public class TelemetryTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(TelemetryTask.class, LogUtil.Category.TASK);

    private final GatewayDiscordClient gateway;

    public TelemetryTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return !Config.IS_SNAPSHOT;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling Telemetry task");
        final GatewayClientGroup group = this.gateway.getGatewayClientGroup();
        return Flux.interval(Duration.ZERO, Duration.ofSeconds(15), scheduler)
                .doOnNext(__ -> {
                    LOGGER.debug("Updating Telemetry statistics");
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
    }

}
