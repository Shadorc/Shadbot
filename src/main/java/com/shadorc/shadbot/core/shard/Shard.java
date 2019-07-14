package com.shadorc.shadbot.core.shard;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.TextUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

public class Shard {

    private final DiscordClient client;
    private final AtomicBoolean isFullyReady;
    private final Logger logger;

    public Shard(DiscordClient client) {
        this.client = client;
        this.isFullyReady = new AtomicBoolean(false);
        this.logger = Loggers.getLogger(String.format("shadbot.shard.%d",
                this.client.getConfig().getShardIndex()));

        this.registerReadyEvent();
        this.registerFullyReadyEvent();

        this.register(new GatewayLifecycleListener(this.logger, this.isFullyReady));
        this.register(new TextChannelDeleteListener());
        this.register(new GuildCreateListener());
        this.register(new GuildDeleteListener());
        this.register(new MemberListener.MemberJoinListener());
        this.register(new MemberListener.MemberLeaveListener());
        this.register(new MessageCreateListener());
        this.register(new MessageUpdateListener());
        this.register(new VoiceStateUpdateListener());
        this.register(new ReactionListener.ReactionAddListener());
        this.register(new ReactionListener.ReactionRemoveListener());
    }

    private void registerReadyEvent() {
        this.client.getEventDispatcher()
                .on(ReadyEvent.class)
                .next()
                .doOnNext(event -> this.logger.info("Presence update scheduled."))
                .flatMapMany(event -> Flux.interval(Duration.ZERO, Duration.ofMinutes(30), Schedulers.elastic())
                        .flatMap(ignored -> {
                            final String presence = String.format("%shelp | %s", Config.DEFAULT_PREFIX, Utils.randValue(TextUtils.TIP_MESSAGES));
                            return event.getClient().updatePresence(Presence.online(Activity.playing(presence)));
                        })
                        .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(event.getClient(), err)))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
    }

    private void registerFullyReadyEvent() {
        this.client.getEventDispatcher()
                .on(ReadyEvent.class)
                .map(event -> event.getGuilds().size())
                .flatMap(size -> this.client.getEventDispatcher()
                        .on(GuildCreateEvent.class)
                        .take(size)
                        .collectList())
                .doOnNext(guilds -> {
                    this.logger.info("Fully ready.");
                    this.isFullyReady.set(true);
                    Shadbot.onFullyReadyEvent(this.client);
                })
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
    }

    private <T extends Event> void register(EventListener<T> eventListener) {
        this.client.getEventDispatcher()
                .on(eventListener.getEventType())
                .flatMap(event -> eventListener.execute(event)
                        .thenReturn(event.toString())
                        .elapsed()
                        .doOnNext(tuple -> {
                            if (this.logger.isTraceEnabled()) {
                                this.logger.trace("{} took {}ms to be processed.", tuple.getT2(), tuple.getT1());
                            }
                            if (tuple.getT1() > Duration.ofMinutes(1).toMillis()) {
                                this.logger.warn("{} took a long time to be processed ({}ms).", tuple.getT2(), tuple.getT1());
                            }
                        })
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.client, err))))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
    }

    public DiscordClient getClient() {
        return this.client;
    }

    public boolean isFullyReady() {
        return this.isFullyReady.get();
    }

}
