package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.command.game.lottery.LotteryCmd;
import com.shadorc.shadbot.command.owner.ResourceStatsCmd;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.TextUtils;
import discord4j.common.ReactorResources;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.Event;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.util.Snowflake;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.api.noop.NoOpStoreService;
import discord4j.store.jdk.JdkStoreService;
import io.sentry.Sentry;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class Shadbot {

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    private static final Instant LAUNCH_TIME = Instant.now();
    private static final AtomicLong OWNER_ID = new AtomicLong();
    private static final AtomicLong SELF_ID = new AtomicLong();

    private static GatewayDiscordClient gateway;
    private static BotListStats botListStats;

    public static void main(String[] args) {
        // Set default to Locale US
        Locale.setDefault(Locale.US);

        LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        LOGGER.info("Initializing Sentry...");
        Sentry.init(CredentialManager.getInstance().get(Credential.SENTRY_DSN));

        // BlockHound is used to detect blocking actions in non-blocking threads
        LOGGER.info("Initializing BlockHound...");
        BlockHound.builder()
                .allowBlockingCallsInside("java.io.FileInputStream", "readBytes")
                .install();

        final DiscordClient client = DiscordClient.builder(CredentialManager.getInstance().get(Credential.DISCORD_TOKEN))
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .setReactorResources(ReactorResources.builder()
                        .timerTaskScheduler(Schedulers.boundedElastic())
                        .build())
                .build();

        LOGGER.info("Acquiring owner ID and self ID...");
        client.getApplicationInfo()
                .map(ApplicationInfoData::owner)
                .map(UserData::id)
                .map(Snowflake::asLong)
                .doOnNext(ownerId -> {
                    LOGGER.info("Owner ID acquired: {}", ownerId);
                    Shadbot.OWNER_ID.set(ownerId);
                })
                .block();

        final long selfId = DiscordUtils.extractSelfId(CredentialManager.getInstance().get(Credential.DISCORD_TOKEN));
        LOGGER.info("Self ID acquired: {}", selfId);
        Shadbot.SELF_ID.set(selfId);

        LOGGER.info("Connecting to Discord...");
        Shadbot.gateway = client.gateway()
                .setEventDispatcher(EventDispatcher.replayingWithSize(0))
                .setStoreService(MappingStoreService.create()
                        // Do not store messages
                        .setMapping(new NoOpStoreService(), MessageData.class)
                        .setFallback(new JdkStoreService()))
                .setInitialStatus(shardInfo -> Presence.idle(Activity.playing("Connecting...")))
                .connect()
                .block();

        LOGGER.info("Scheduling presence updates...");
        Flux.interval(Duration.ZERO, Duration.ofMinutes(30), Schedulers.boundedElastic())
                .flatMap(ignored -> {
                    final String presence = String.format("%shelp | %s", Config.DEFAULT_PREFIX,
                            TextUtils.TIPS.getRandomTextFormatted());
                    return Shadbot.gateway.updatePresence(Presence.online(Activity.playing(presence)));
                })
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);

        LOGGER.info("Starting lottery... Next lottery draw in {}", LotteryCmd.getDelay());
        Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), Schedulers.boundedElastic())
                .flatMap(ignored -> LotteryCmd.draw(Shadbot.gateway))
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);

        LOGGER.info("Starting bot list stats scheduler...");
        Shadbot.botListStats = new BotListStats();

        LOGGER.info("Scheduling system resources log...");
        Flux.interval(Duration.ZERO, ResourceStatsCmd.UPDATE_INTERVAL, Schedulers.boundedElastic())
                .flatMap(ignored -> DatabaseManager.getStats().logSystemResources())
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(err))
                .subscribe(null, ExceptionHandler::handleUnknownError);

        LOGGER.info("Registering listeners...");
        Shadbot.register(Shadbot.gateway, new TextChannelDeleteListener());
        Shadbot.register(Shadbot.gateway, new GuildCreateListener());
        Shadbot.register(Shadbot.gateway, new GuildDeleteListener());
        Shadbot.register(Shadbot.gateway, new MemberListener.MemberJoinListener());
        Shadbot.register(Shadbot.gateway, new MemberListener.MemberLeaveListener());
        Shadbot.register(Shadbot.gateway, new MessageCreateListener());
        Shadbot.register(Shadbot.gateway, new MessageUpdateListener());
        Shadbot.register(Shadbot.gateway, new VoiceStateUpdateListener());
        Shadbot.register(Shadbot.gateway, new ReactionListener.ReactionAddListener());
        Shadbot.register(Shadbot.gateway, new ReactionListener.ReactionRemoveListener());

        LOGGER.info("Shadbot is fully connected!");

        Shadbot.gateway.onDisconnect().block();
    }

    private static <T extends Event> void register(GatewayDiscordClient client, EventListener<T> eventListener) {
        client.getEventDispatcher()
                .on(eventListener.getEventType())
                .flatMap(event -> eventListener.execute(event)
                        .thenReturn(event.toString())
                        .elapsed()
                        .doOnNext(tuple -> {
                            if (LOGGER.isTraceEnabled()) {
                                LOGGER.trace("{} took {} to be processed: {}",
                                        eventListener.getEventType().getSimpleName(), FormatUtils.shortDuration(tuple.getT1()),
                                        tuple.getT2());
                            } else if (tuple.getT1() > Duration.ofMinutes(1).toMillis()) {
                                LOGGER.warn("{} took {} to be processed.",
                                        eventListener.getEventType().getSimpleName(), FormatUtils.shortDuration(tuple.getT1()));
                            }
                        })
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * @return The time when this class was loaded.
     */
    public static Instant getLaunchTime() {
        return Shadbot.LAUNCH_TIME;
    }

    /**
     * @return The ID of the owner.
     */
    public static Snowflake getOwnerId() {
        return Snowflake.of(Shadbot.OWNER_ID.get());
    }

    /**
     * @return The ID of the bot.
     */
    public static Snowflake getSelfId() {
        return Snowflake.of(Shadbot.SELF_ID.get());
    }

    public static GatewayDiscordClient getClient() {
        return Shadbot.gateway;
    }

    public static Mono<Void> quit() {
        if (Shadbot.botListStats != null) {
            Shadbot.botListStats.stop();
        }

        return Shadbot.gateway.logout()
                .then(Mono.fromRunnable(() -> DatabaseManager.getInstance().close()));
    }

}
