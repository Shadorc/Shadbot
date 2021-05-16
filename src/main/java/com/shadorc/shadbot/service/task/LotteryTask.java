package com.shadorc.shadbot.service.task;

import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.LogUtil;
import discord4j.core.GatewayDiscordClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.util.Logger;

import java.time.Duration;

public class LotteryTask implements Task {

    private static final Logger LOGGER = LogUtil.getLogger(LotteryTask.class, LogUtil.Category.TASK);

    private final GatewayDiscordClient gateway;

    public LotteryTask(GatewayDiscordClient gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Disposable schedule(Scheduler scheduler) {
        LOGGER.info("Scheduling Lottery task (next draw in {})",
                FormatUtil.formatDurationWords(Config.DEFAULT_LOCALE, LotteryCmd.getDelay()));
        return Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), scheduler)
                .flatMap(__ -> LotteryCmd.draw(this.gateway)
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

}
