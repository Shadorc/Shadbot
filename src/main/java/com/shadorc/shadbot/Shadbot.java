package com.shadorc.shadbot;

import com.shadorc.shadbot.core.retriever.DistinctFallbackEntityRetriever;
import com.shadorc.shadbot.core.retriever.SpyRestEntityRetriever;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtils;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.UserData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.util.Snowflake;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import io.prometheus.client.exporter.HTTPServer;
import io.sentry.Sentry;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
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
    private static final AtomicLong SELF_ID = new AtomicLong();

    private static GatewayDiscordClient gateway;
    private static TaskManager taskManager;
    private static HTTPServer prometheusServer;

    public static void main(String[] args) {
        // Set default to Locale US
        Locale.setDefault(Locale.US);

        DEFAULT_LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        final String port = CredentialManager.getInstance().get(Credential.PROMETHEUS_PORT);
        if (port != null) {
            DEFAULT_LOGGER.info("Initializing Prometheus on port {}...", port);
            try {
                Shadbot.prometheusServer = new HTTPServer(Integer.parseInt(port));
            } catch (final IOException err) {
                DEFAULT_LOGGER.error("An error occurred while initializing Prometheus", err);
            }
        }

        if (!Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing Sentry...");
            Sentry.init(CredentialManager.getInstance().get(Credential.SENTRY_DSN))
                    .addShouldSendEventCallback(event -> !event.getLogger().startsWith("com.sedmelluq"));
        }

        // BlockHound is used to detect blocking actions in non-blocking threads
        if (Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing BlockHound...");
            BlockHound.builder()
                    .allowBlockingCallsInside("java.io.FileInputStream", "readBytes")
                    .install();
        }

        final DiscordClient client = DiscordClient.builder(CredentialManager.getInstance().get(Credential.DISCORD_TOKEN))
                .setDebugMode(Config.IS_SNAPSHOT)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .build();

        DEFAULT_LOGGER.info("Acquiring owner ID and self ID...");
        client.getApplicationInfo()
                .map(ApplicationInfoData::owner)
                .map(UserData::id)
                .map(Snowflake::asLong)
                .doOnNext(ownerId -> {
                    DEFAULT_LOGGER.info("Owner ID acquired: {}", ownerId);
                    Shadbot.OWNER_ID.set(ownerId);
                })
                .block();

        final long selfId = DiscordUtils.extractSelfId(CredentialManager.getInstance().get(Credential.DISCORD_TOKEN));
        DEFAULT_LOGGER.info("Self ID acquired: {}", selfId);
        Shadbot.SELF_ID.set(selfId);

        DEFAULT_LOGGER.info("Connecting to Discord...");
        client.gateway()
                .setAwaitConnections(false)
                .setEntityRetrievalStrategy(gateway -> new DistinctFallbackEntityRetriever(
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
                .setInitialStatus(ignored -> DiscordUtils.getRandomStatus())
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    Shadbot.gateway = gateway;

                    Shadbot.taskManager = new TaskManager(gateway);
                    Shadbot.taskManager.scheduleLottery();
                    Shadbot.taskManager.schedulePeriodicStats();
                    Shadbot.taskManager.schedulePresenceUpdates();
                    if (!Config.IS_SNAPSHOT) {
                        Shadbot.taskManager.schedulePostStats();
                        Shadbot.taskManager.scheduleVotersCheck();
                    }

                    DEFAULT_LOGGER.info("Registering listeners...");
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

                    return gateway.onDisconnect();
                })
                .block();

        System.exit(0);
    }

    private static <T extends Event> void register(GatewayDiscordClient gateway, EventListener<T> eventListener) {
        gateway.getEventDispatcher()
                .on(eventListener.getEventType())
                .flatMap(event -> eventListener.execute(event)
                        .thenReturn(event.toString())
                        .filter(ignored -> DEFAULT_LOGGER.isTraceEnabled())
                        .elapsed()
                        .doOnNext(tuple -> DEFAULT_LOGGER.trace("{} took {} to be processed: {}",
                                eventListener.getEventType().getSimpleName(), FormatUtils.shortDuration(tuple.getT1()),
                                tuple.getT2()))
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

    public static Mono<Void> quit() {
        if (Shadbot.prometheusServer != null) {
            Shadbot.prometheusServer.stop();
        }
        if (Shadbot.taskManager != null) {
            Shadbot.taskManager.stop();
        }

        return Shadbot.gateway.logout()
                .then(Mono.fromRunnable(() -> DatabaseManager.getInstance().close()));
    }

}
