package com.locibot.locibot;

import com.locibot.locibot.command.game.lottery.LotteryCmd;
import com.locibot.locibot.api.BotListStats;
import com.locibot.locibot.command.group.GroupUtil;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.database.DatabaseManager;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
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

    public void scheduleGroups(GatewayDiscordClient gateway) {
        LOGGER.info("Scheduling group reminder");
        final Disposable task = Flux.interval(Duration.ofMinutes(5), Duration.ofMinutes(5), DEFAULT_SCHEDULER)
                .flatMap(__ -> {
                    LOGGER.info("Starting group reminder");
                    DatabaseManager.getGroups().getAllGroups().forEach(group -> {
                        String date = group.getBean().getScheduledDate();
                        //check if group is scheduled
                        if (date != null) {
                            //check if date_now == group scheduled date
                            if (LocalDate.parse(date).isEqual(LocalDate.now())
                                    //send invite to all members of group, if (time_now + 35min) > scheduled_time > (time_now + 25min)
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isAfter(LocalTime.now().plus(1950, ChronoUnit.SECONDS))
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isBefore(LocalTime.now().plus(1650, ChronoUnit.SECONDS))) {
                                LOGGER.info("30min reminder for group " + group.getGroupName());
                                group.getMembers().forEach(member -> {
                                    if (member.getBean().getAccepted() == 0)
                                        gateway.getUserById(member.getId()).flatMap(user ->
                                                user.getPrivateChannel().flatMap(privateChannel ->
                                                        privateChannel.createEmbed(GroupUtil.sendInviteMessage(group, gateway.getUserById(member.getId()).block()))
                                                                .then(group.updateInvited(member.getId(), true).then(group.updateAccept(member.getId(), 0)))
                                                )).subscribe();
                                });
                            } else if (LocalDate.parse(date).isEqual(LocalDate.now())
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isBefore(LocalTime.now().plus(3750, ChronoUnit.SECONDS))
                                    && LocalTime.parse(group.getBean().getScheduledTime()).isAfter(LocalTime.now().plus(3450, ChronoUnit.SECONDS))) {
                                LOGGER.info("1h reminder for group " + group.getGroupName());
                                group.getMembers().forEach(member -> {
                                    if (member.getBean().isInvited() && member.getBean().getAccepted() == 0)
                                        gateway.getUserById(member.getId()).flatMap(user ->
                                                user.getPrivateChannel().flatMap(privateChannel ->
                                                        privateChannel.createEmbed(GroupUtil.sendInviteMessage(group, gateway.getUserById(member.getId()).block()))
                                                )).subscribe();
                                });
                            }

                        }
                    });
                    LOGGER.info("Closing group reminder");
                    return Mono.empty();
                })
                .subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void scheduleDeleteOldGroups() {
        LOGGER.info("Scheduling group disposer");
        final Disposable task = Flux.interval(Duration.ofMinutes(1), Duration.ofHours(24), DEFAULT_SCHEDULER)
                .flatMap(__ -> {
                    LOGGER.info("Starting group disposer");
                    DatabaseManager.getGroups().getAllGroups().forEach(group -> {
                        String date = group.getBean().getScheduledDate();
                        if (date != null && LocalDate.now().isAfter(LocalDate.parse(date))) {
                            group.delete().subscribe();
                            LOGGER.info("Group " + group.getGroupName() + " deleted");
                        }
                    });
                    LOGGER.info("Closing group disposer");
                    return Mono.empty();
                }).subscribe(null, ExceptionHandler::handleUnknownError);
        this.tasks.add(task);
    }

    public void stop() {
        this.tasks.forEach(Disposable::dispose);
    }

}
