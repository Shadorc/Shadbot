package com.locibot.locibot;

import com.locibot.locibot.database.DatabaseManager;
import com.locibot.locibot.listener.*;
import com.locibot.locibot.api.BotListStats;
import com.locibot.locibot.core.command.CommandManager;
import com.locibot.locibot.core.retriever.SpyRestEntityRetriever;
import com.locibot.locibot.data.Config;
import com.locibot.locibot.data.Telemetry;
import com.locibot.locibot.data.credential.Credential;
import com.locibot.locibot.data.credential.CredentialManager;
import com.locibot.locibot.listener.*;
import com.locibot.locibot.object.ExceptionHandler;
import com.locibot.locibot.utils.FormatUtil;
import com.locibot.locibot.utils.LogUtil;
import discord4j.common.ReactorResources;
import discord4j.common.store.Store;
import discord4j.common.store.legacy.LegacyStoreLayout;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.presence.ClientActivity;
import discord4j.core.object.presence.ClientPresence;
import discord4j.core.retriever.EntityRetrievalStrategy;
import discord4j.core.retriever.FallbackEntityRetriever;
import discord4j.core.shard.MemberRequestFilter;
import discord4j.discordjson.json.ApplicationInfoData;
import discord4j.discordjson.json.MessageData;
import discord4j.gateway.intent.Intent;
import discord4j.gateway.intent.IntentSet;
import discord4j.rest.response.ResponseFunction;
import discord4j.rest.util.AllowedMentions;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import io.netty.resolver.DefaultAddressResolverGroup;
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

public class LociBot {

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
                LociBot.prometheusServer = new HTTPServer(Integer.parseInt(prometheusPort));
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

        ReactorResources reactor = ReactorResources.builder()
                .httpClient(ReactorResources.DEFAULT_HTTP_CLIENT.get()
                        .resolver(DefaultAddressResolverGroup.INSTANCE))
                .build();

        final DiscordClient client = DiscordClient.builder(discordToken)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .setDefaultAllowedMentions(AllowedMentions.suppressEveryone())
                .setReactorResources(reactor)
                .build();

        final ApplicationInfoData applicationInfo = client.getApplicationInfo().block();
        Objects.requireNonNull(applicationInfo);
        LociBot.OWNER_ID.set(Snowflake.asLong(applicationInfo.owner().id()));
        final long applicationId = Snowflake.asLong(applicationInfo.id());
        DEFAULT_LOGGER.info("Owner ID: {} | Application ID: {}", LociBot.OWNER_ID.get(), applicationId);

        DEFAULT_LOGGER.info("Registering commands");
        CommandManager.register(client.getApplicationService(), applicationId).block();

        DEFAULT_LOGGER.info("Connecting to Discord");
        client.gateway()
                .setStore(Store.fromLayout(LegacyStoreLayout.of(MappingStoreService.create()
                        // Stores messages during 15 minutes
                        .setMapping(new CaffeineStoreService(
                                builder -> builder.expireAfterWrite(Duration.ofMinutes(15))), MessageData.class)
                        .setFallback(new JdkStoreService()))))
                .setEntityRetrievalStrategy(gateway -> new FallbackEntityRetriever(
                        EntityRetrievalStrategy.STORE.apply(gateway), new SpyRestEntityRetriever(gateway)))
                .setEnabledIntents(IntentSet.of(
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.GUILD_MESSAGES,
                        Intent.DIRECT_MESSAGES))
                .setInitialPresence(__ -> ClientPresence.online(ClientActivity.listening("/help")))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    LociBot.gateway = gateway;

                    LociBot.taskManager = new TaskManager();
                    LociBot.taskManager.scheduleLottery(gateway);
                    LociBot.taskManager.schedulePeriodicStats(gateway);

                    if (!Config.IS_SNAPSHOT) {
                        if (CredentialManager.get(Credential.BOTLIST_DOT_SPACE_TOKEN) != null) {
                            DEFAULT_LOGGER.info("Initializing BotListStats");
                            LociBot.botListStats = new BotListStats(gateway);
                            LociBot.taskManager.schedulePostStats(LociBot.botListStats);
                        }
                    }

                    DEFAULT_LOGGER.info("Registering listeners");
                    /* Intent.GUILDS */
                    LociBot.register(gateway, new GuildCreateListener());
                    LociBot.register(gateway, new GuildDeleteListener());
                    LociBot.register(gateway, new RoleDeleteListener());
                    LociBot.register(gateway, new ChannelDeleteListener.TextChannelDeleteListener());
                    LociBot.register(gateway, new ChannelDeleteListener.VoiceChannelDeleteListener());
                    /* Intent.GUILD_MEMBERS */
                    LociBot.register(gateway, new MemberJoinListener());
                    LociBot.register(gateway, new MemberLeaveListener());
                    /* Intent.GUILD_VOICE_STATES */
                    LociBot.register(gateway, new VoiceStateUpdateListener());
                    /* Intent.GUILD_MESSAGE_REACTIONS */
                    LociBot.register(gateway, new ReactionListener.ReactionAddListener());
                    LociBot.register(gateway, new ReactionListener.ReactionRemoveListener());
                    LociBot.register(gateway, new InteractionCreateListener());

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
                                .formatted(FormatUtil.formatDurationWords(Config.DEFAULT_LOCALE, EVENT_TIMEOUT), event))))
                        .onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(err))))
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    /**
     * @return The ID of the owner.
     */
    public static Snowflake getOwnerId() {
        return Snowflake.of(LociBot.OWNER_ID.get());
    }

    public static Mono<Void> quit() {
        return Mono.defer(() -> {
            DEFAULT_LOGGER.info("Shutdown request received");

            if (LociBot.prometheusServer != null) {
                LociBot.prometheusServer.stop();
            }
            if (LociBot.taskManager != null) {
                LociBot.taskManager.stop();
            }
            if (LociBot.botListStats != null) {
                LociBot.botListStats.stop();
            }

            return LociBot.gateway.logout()
                    .then(Mono.fromRunnable(DatabaseManager::close));
        });
    }

}
