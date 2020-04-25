package com.shadorc.shadbot;

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
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
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
import io.sentry.Sentry;
import reactor.blockhound.BlockHound;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

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

    public static void main(String[] args) {
        // Set default to Locale US
        Locale.setDefault(Locale.US);

        DEFAULT_LOGGER.info("Starting Shadbot V{}", Config.VERSION);

        if (!Config.IS_SNAPSHOT) {
            DEFAULT_LOGGER.info("Initializing Sentry...");
            Sentry.init(CredentialManager.getInstance().get(Credential.SENTRY_DSN));
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
        Shadbot.gateway = client.gateway()
                .setEntityRetrievalStrategy(EntityRetrievalStrategy.STORE_FALLBACK_REST)
                .setEnabledIntents(IntentSet.of(
                        // TODO: Remove once fixed: https://github.com/Discord4J/Discord4J/issues/660
                        Intent.GUILD_PRESENCES,
                        Intent.GUILDS,
                        Intent.GUILD_MEMBERS,
                        Intent.GUILD_VOICE_STATES,
                        Intent.GUILD_MESSAGES,
                        Intent.GUILD_MESSAGE_REACTIONS,
                        Intent.DIRECT_MESSAGES))
                .setStoreService(MappingStoreService.create()
                        // Stores messages during 1 hour
                        .setMapping(new CaffeineStoreService(
                                builder -> builder.expireAfterWrite(Duration.ofHours(1))), MessageData.class)
                        .setFallback(new JdkStoreService()))
                .setInitialStatus(shardInfo -> Presence.idle(Activity.playing("Connecting...")))
                .setMemberRequestFilter(MemberRequestFilter.none())
                .login()
                .block();

        Shadbot.taskManager = new TaskManager(gateway);
        Shadbot.taskManager.schedulesPresenceUpdates();
        Shadbot.taskManager.schedulesLottery();
        Shadbot.taskManager.schedulesPostStats();
        Shadbot.taskManager.schedulesSystemResourcesLog();

        DEFAULT_LOGGER.info("Registering listeners...");
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

        DEFAULT_LOGGER.info("Shadbot is fully connected");

        Shadbot.gateway.onDisconnect().block();
        System.exit(0);
    }

    private static <T extends Event> void register(GatewayDiscordClient client, EventListener<T> eventListener) {
        client.getEventDispatcher()
                .on(eventListener.getEventType())
                .flatMap(event -> eventListener.execute(event)
                        .thenReturn(event.toString())
                        .elapsed()
                        .doOnNext(tuple -> {
                            if (DEFAULT_LOGGER.isTraceEnabled()) {
                                DEFAULT_LOGGER.trace("{} took {} to be processed: {}",
                                        eventListener.getEventType().getSimpleName(), FormatUtils.shortDuration(tuple.getT1()),
                                        tuple.getT2());
                            } else if (tuple.getT1() > Duration.ofMinutes(1).toMillis()) {
                                DEFAULT_LOGGER.warn("{} took {} to be processed",
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

    public static Mono<Void> quit() {
        if (Shadbot.taskManager != null) {
            Shadbot.taskManager.stop();
        }

        return Shadbot.gateway.logout()
                .then(Mono.fromRunnable(() -> DatabaseManager.getInstance().close()));
    }

}
