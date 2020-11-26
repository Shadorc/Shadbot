package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.core.retriever.SpyRestEntityRetriever;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.retriever.FallbackEntityRetriever;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.response.ResponseFunction;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import io.prometheus.client.exporter.HTTPServer;
import io.sentry.Sentry;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class Shadbot {

    public static final Logger DEFAULT_LOGGER = Loggers.getLogger("shadbot");

    private static final Instant LAUNCH_TIME = Instant.now();
    private static final AtomicLong OWNER_ID = new AtomicLong();

    private static GatewayDiscordClient gateway;
    private static TaskManager taskManager;
    private static HTTPServer prometheusServer;
    private static BotListStats botListStats;

    public static void main(String[] args) {
        // Set default to Locale US
        Locale.setDefault(Locale.US);

        if (!Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing Sentry");
            Sentry.init(options -> {
                options.setDsn(CredentialManager.getInstance().get(Credential.SENTRY_DSN));
                options.setRelease(Config.VERSION);
                // Ignore events coming from lavaplayer
                options.setBeforeSend(
                        (sentryEvent, obj) -> sentryEvent.getLogger().startsWith("com.sedmelluq") ? null : sentryEvent);
            });
        }

        DEFAULT_LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        final String port = CredentialManager.getInstance().get(Credential.PROMETHEUS_PORT);
        if (port != null) {
            DEFAULT_LOGGER.info("Initializing Prometheus on port {}", port);
            try {
                Shadbot.prometheusServer = new HTTPServer(Integer.parseInt(port));
            } catch (final IOException err) {
                DEFAULT_LOGGER.error("An error occurred while initializing Prometheus", err);
            }
        }

        if (Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("[SNAPSHOT] Initializing Reactor operator stack recorder");
            Hooks.onOperatorDebug();
        }

        final DiscordClient client = DiscordClient.builder(CredentialManager.getInstance().get(Credential.DISCORD_TOKEN))
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .build();

        client.getApplicationInfo()
                .map(ApplicationInfoData::owner)
                .map(UserData::id)
                .map(Snowflake::asLong)
                .doOnNext(ownerId -> {
                    DEFAULT_LOGGER.info("Owner ID acquired: {}", ownerId);
                    Shadbot.OWNER_ID.set(ownerId);
                })
                .block();

        DEFAULT_LOGGER.info("Connecting to Discord");
        client.gateway()
                .setAwaitConnections(false)
                .setEntityRetrievalStrategy(gateway -> new FallbackEntityRetriever(
                        EntityRetrievalStrategy.STORE.apply(gateway), new SpyRestEntityRetriever(gateway)))
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES))
                .setStoreService(MappingStoreService.create()
                        // Stores messages during 30 minutes
                        .setMapping(new CaffeineStoreService(
                                builder -> builder.expireAfterWrite(Duration.ofMinutes(30))), MessageData.class)
                        .setFallback(new JdkStoreService()))
                .setInitialStatus(ignored -> ShadbotUtils.getRandomStatus())
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    Shadbot.gateway = gateway;

                    Shadbot.taskManager = new TaskManager();
                    Shadbot.taskManager.scheduleLottery(gateway);
                    Shadbot.taskManager.schedulePeriodicStats(gateway);
                    Shadbot.taskManager.schedulePresenceUpdates(gateway);

                    if (!Config.IS_SNAPSHOT) {
                        DEFAULT_LOGGER.info("Initializing BotListStats");
                        Shadbot.botListStats = new BotListStats(gateway);

                        Shadbot.taskManager.schedulePostStats(Shadbot.botListStats);
                    }

                    DEFAULT_LOGGER.info("Registering listeners");
                    Shadbot.register(gateway, new TextChannelDeleteListener());
                    Shadbot.register(gateway, new GuildCreateListener());
                    Shadbot.register(gateway, new GuildDeleteListener());
                    Shadbot.register(gateway, new MemberJoinListener());
                    Shadbot.register(gateway, new MemberLeaveListener());
                    Shadbot.register(gateway, new MessageCreateListener());
                    Shadbot.register(gateway, new MessageUpdateListener());
                    Shadbot.register(gateway, new VoiceStateUpdateListener());
                    Shadbot.register(gateway, new ReactionListener.ReactionAddListener());
                    Shadbot.register(gateway, new ReactionListener.ReactionRemoveListener());

                    DEFAULT_LOGGER.info("Shadbot is ready");
                    return gateway.onDisconnect();
                })
                .block();

        System.exit(0);
    }

    private static <T extends Event> void register(GatewayDiscordClient gateway, EventListener<T> eventListener) {
        gateway.getEventDispatcher()
                .on(eventListener.getEventType())
                .flatMap(event -> eventListener.execute(event)
                        .thenReturn(eventListener.getEventType().getSimpleName())
                        .filter(ignored -> DEFAULT_LOGGER.isTraceEnabled())
                        .elapsed()
                        .doOnNext(TupleUtils.consumer((elapsed, eventType) ->
                                DEFAULT_LOGGER.trace("{} took {} to be processed: {}",
                                        eventType, FormatUtils.formatDuration(elapsed), event.toString())))
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

    public static Mono<Void> quit() {
        if (Shadbot.prometheusServer != null) {
            Shadbot.prometheusServer.stop();
        }
        if (Shadbot.taskManager != null) {
            Shadbot.taskManager.stop();
        }
        if (Shadbot.botListStats != null) {
            Shadbot.botListStats.stop();
        }

        return Shadbot.gateway.logout()
                .then(Mono.fromRunnable(() -> DatabaseManager.getInstance().close()));
    }

}
