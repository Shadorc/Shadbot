package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.core.retriever.SpyRestEntityRetriever;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.LogUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.retriever.FallbackEntityRetriever;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.MessageData;
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
import reactor.util.Logger;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Shadbot {

    public static final Logger DEFAULT_LOGGER = LogUtil.getLogger();

    private static final Duration EVENT_TIMEOUT = Duration.ofHours(12);
    private static final AtomicLong OWNER_ID = new AtomicLong();

    private static GatewayDiscordClient gateway;
    private static TaskManager taskManager;
    private static HTTPServer prometheusServer;
    private static BotListStats botListStats;

    public static void main(String[] args) {
        Locale.setDefault(Config.DEFAULT_LOCALE);

        final String sentryDsn = CredentialManager.get(Credential.SENTRY_DSN);
        if (sentryDsn != null && !Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing Sentry");
            Sentry.init(options -> {
                options.setDsn(sentryDsn);
                options.setRelease(Config.VERSION);
                // Ignore events coming from lavaplayer
                options.setBeforeSend(
                        (sentryEvent, obj) -> sentryEvent.getLogger().startsWith("com.sedmelluq") ? null : sentryEvent);
            });
        }

        DEFAULT_LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        final String prometheusPort = CredentialManager.get(Credential.PROMETHEUS_PORT);
        if (prometheusPort != null && !Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing Prometheus on port {}", prometheusPort);
            try {
                Shadbot.prometheusServer = new HTTPServer(Integer.parseInt(prometheusPort));
            } catch (final IOException err) {
                DEFAULT_LOGGER.error("An error occurred while initializing Prometheus", err);
            }
        }

        if (Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("[SNAPSHOT] Enabling Reactor operator stack recorder");
            Hooks.onOperatorDebug();
        }

        final String discordToken = CredentialManager.get(Credential.DISCORD_TOKEN);
        Objects.requireNonNull(discordToken, "Missing Discord bot token");
        final DiscordClient client = DiscordClient.builder(discordToken)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .build();

        final ApplicationInfoData applicationInfo = client.getApplicationInfo().block();
        Objects.requireNonNull(applicationInfo);
        Shadbot.OWNER_ID.set(Snowflake.asLong(applicationInfo.owner().id()));
        final long applicationId = Snowflake.asLong(applicationInfo.id());
        DEFAULT_LOGGER.info("Owner ID: {} | Application ID: {}", Shadbot.OWNER_ID.get(), applicationId);

        DEFAULT_LOGGER.info("Registering commands");
        //CommandManager.getInstance().register(client.getApplicationService(), applicationId).block();

        DEFAULT_LOGGER.info("Connecting to Discord");
        client.gateway()
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
                        // TODO: Is it still useful?
                        .setMapping(new CaffeineStoreService(
                                builder -> builder.expireAfterWrite(Duration.ofMinutes(30))), MessageData.class)
                        .setFallback(new JdkStoreService()))
                .setInitialStatus(__ -> ShadbotUtil.getRandomStatus())
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    Shadbot.gateway = gateway;

                    /*gateway.getRestClient()
                            .getApplicationService()
                            .getGuildApplicationCommands(applicationId, Config.OWNER_GUILD_ID)
                            .doOnNext(command -> DEFAULT_LOGGER.info("Deleting {}", command.name()))
                            .flatMap(commandData -> gateway.getRestClient()
                                    .getApplicationService()
                                    .deleteGuildApplicationCommand(applicationId, Config.OWNER_GUILD_ID, Long.parseLong(commandData.id()))
                                    .doOnTerminate(() -> DEFAULT_LOGGER.info("Deleted: {}", commandData.name())))
                            .doOnTerminate(() -> DEFAULT_LOGGER.info("Done!"))
                            .subscribe();*/

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
                    Shadbot.register(gateway, new VoiceStateUpdateListener());
//                    Shadbot.register(gateway, new ReactionListener.ReactionAddListener());
//                    Shadbot.register(gateway, new ReactionListener.ReactionRemoveListener());
                    Shadbot.register(gateway, new InteractionCreateListener());

                    DEFAULT_LOGGER.info("Shadbot is ready");
                    return gateway.onDisconnect();
                })
                .block();

        System.exit(0);
    }

    private static <T extends Event> void register(GatewayDiscordClient gateway, EventListener<T> eventListener) {
        gateway.getEventDispatcher()
                .on(eventListener.getEventType())
                .doOnNext(event -> Telemetry.EVENT_COUNTER.labels(event.getClass().getSimpleName()).inc())
                .flatMap(event -> eventListener.execute(event)
                        .timeout(EVENT_TIMEOUT, Mono.error(new RuntimeException("Event timed out after %s: %s"
                                .formatted(FormatUtil.formatDurationWords(EVENT_TIMEOUT), event))))
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * @return The ID of the owner.
     */
    public static Snowflake getOwnerId() {
        return Snowflake.of(Shadbot.OWNER_ID.get());
    }

    public static Mono<Void> quit() {
        DEFAULT_LOGGER.info("Shutdown request received");

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
                .then(Mono.fromRunnable(DatabaseManager::close));
    }

}
