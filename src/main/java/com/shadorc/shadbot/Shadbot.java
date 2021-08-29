package com.shadorc.shadbot;

import com.shadorc.shadbot.core.command.CommandManager;
import com.shadorc.shadbot.core.retriever.SpyRestEntityRetriever;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.Telemetry;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.listener.*;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.service.ServiceManager;
import com.shadorc.shadbot.service.TaskService;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.LogUtil;
import discord4j.common.store.Store;
import discord4j.common.store.legacy.LegacyStoreLayout;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.Event;
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
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Shadbot {

    public static final Logger DEFAULT_LOGGER = LogUtil.getLogger();

    private static final Duration EVENT_TIMEOUT = Duration.ofHours(12);
    private static final AtomicLong OWNER_ID = new AtomicLong();

    private static GatewayDiscordClient gateway;
    private static ServiceManager serviceManager;

    public static void main(String[] args) {
        Locale.setDefault(Config.DEFAULT_LOCALE);

        DEFAULT_LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        Shadbot.serviceManager = new ServiceManager();
        Shadbot.serviceManager.start();

        if (Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("[SNAPSHOT] Enabling Reactor operator stack recorder");
            Hooks.onOperatorDebug();
        }

        final String discordToken = CredentialManager.get(Credential.DISCORD_TOKEN);
        Objects.requireNonNull(discordToken, "Missing Discord bot token");
        final DiscordClient client = DiscordClient.builder(discordToken)
                .onClientResponse(ResponseFunction.emptyIfNotFound())
                .setDefaultAllowedMentions(AllowedMentions.suppressEveryone())
                .build();

        final ApplicationInfoData applicationInfo = client.getApplicationInfo().block();
        Objects.requireNonNull(applicationInfo);
        Shadbot.OWNER_ID.set(Snowflake.asLong(applicationInfo.owner().id()));
        final long applicationId = Snowflake.asLong(applicationInfo.id());
        DEFAULT_LOGGER.info("Owner ID: {} | Application ID: {}", Shadbot.OWNER_ID.get(), applicationId);

        DEFAULT_LOGGER.info("Registering commands");
        CommandManager.register(client.getApplicationService(), applicationId).block();

        DEFAULT_LOGGER.info("Connecting to Discord");
        client.gateway()
                .setStore(Store.fromLayout(LegacyStoreLayout.of(MappingStoreService.create()
                        // Store messages during 15 minutes
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
                .setInitialPresence(__ -> ClientPresence.online(ClientActivity
                        .listening("/help | Slash commands available!")))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .withGateway(gateway -> {
                    Shadbot.gateway = gateway;

                    final TaskService taskService = new TaskService(gateway);
                    Shadbot.serviceManager.addService(taskService);
                    taskService.start();

                    DEFAULT_LOGGER.info("Registering listeners");
                    /* Intent.GUILDS */
                    Shadbot.register(gateway, new GuildCreateListener());
                    Shadbot.register(gateway, new GuildDeleteListener());
                    Shadbot.register(gateway, new RoleDeleteListener());
                    Shadbot.register(gateway, new ChannelDeleteListener.TextChannelDeleteListener());
                    Shadbot.register(gateway, new ChannelDeleteListener.VoiceChannelDeleteListener());
                    /* Intent.GUILD_MEMBERS */
                    Shadbot.register(gateway, new MemberJoinListener());
                    Shadbot.register(gateway, new MemberLeaveListener());
                    /* Intent.GUILD_VOICE_STATES */
                    Shadbot.register(gateway, new VoiceStateUpdateListener());
                    /* Intent.GUILD_MESSAGE_REACTIONS */
                    Shadbot.register(gateway, new ReactionListener.ReactionAddListener());
                    Shadbot.register(gateway, new ReactionListener.ReactionRemoveListener());
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
                                .formatted(FormatUtil.formatDurationWords(Config.DEFAULT_LOCALE, EVENT_TIMEOUT), event))))
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
        return Mono.defer(() -> {
            DEFAULT_LOGGER.info("Shutdown request received");

            if (Shadbot.serviceManager != null) {
                DEFAULT_LOGGER.info("Stopping services");
                Shadbot.serviceManager.stop();
            }

            DEFAULT_LOGGER.info("Closing gateway discord client");
            return Shadbot.gateway.logout()
                    .then(Mono.fromRunnable(DatabaseManager::close));
        });
    }

}
