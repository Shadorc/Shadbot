package com.shadorc.shadbot;

import com.shadorc.shadbot.command.game.LotteryCmd;
import com.shadorc.shadbot.core.shard.Shard;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.data.database.DatabaseManager;
import com.shadorc.shadbot.data.lottery.LotteryManager;
import com.shadorc.shadbot.data.premium.PremiumManager;
import com.shadorc.shadbot.data.stats.StatsManager;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.lifecycle.ResumeEvent;
import discord4j.core.object.data.stored.MessageBean;
import discord4j.core.object.entity.ApplicationInfo;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import discord4j.core.shard.ShardingClientBuilder;
import discord4j.rest.request.RouterOptions;
import discord4j.rest.response.ResponseFunction;
import discord4j.store.api.mapping.MappingStoreService;
import discord4j.store.caffeine.CaffeineStoreService;
import discord4j.store.jdk.JdkStoreService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Shadbot {

    private static final Instant LAUNCH_TIME = Instant.now();
    private static final AtomicLong OWNER_ID = new AtomicLong();
    private static final AtomicInteger CONNECTED_SHARDS = new AtomicInteger();
    private static final Map<Integer, Shard> SHARDS = new ConcurrentHashMap<>();

    private static BotListStats botListStats;

    public static void main(String[] args) {
        // Set default to Locale US
        Locale.setDefault(Locale.US);

        Runtime.getRuntime().addShutdownHook(new Thread(Shadbot::save));

        LogUtils.info("Next lottery draw in: %s", LotteryCmd.getDelay().toString());
        Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), Schedulers.elastic())
                .flatMap(ignored -> LotteryCmd.draw(Shadbot.getClient()))
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));

        LogUtils.info("Connecting...");
        final Flux<DiscordClient> clients = new ShardingClientBuilder(Credentials.get(Credential.DISCORD_TOKEN))
                .setRouterOptions(RouterOptions.builder()
                        .onClientResponse(ResponseFunction.emptyIfNotFound())
                        .build())
                .setStoreService(MappingStoreService.create()
                        .setMapping(new CaffeineStoreService(caffeine -> caffeine
                                .expireAfterWrite(Duration.ofHours(6))), MessageBean.class)
                        .setFallback(new JdkStoreService()))
                .build()
                .map(builder -> builder
                        .setInitialPresence(Presence.idle(Activity.playing("Connecting..."))))
                .map(DiscordClientBuilder::build)
                .doOnNext(client -> Shadbot.SHARDS.put(client.getConfig().getShardIndex(), new Shard(client)));

        // Get the bot owner ID
        clients.next()
                .flatMapMany(client -> Flux.first(client.getEventDispatcher().on(ReadyEvent.class),
                        client.getEventDispatcher().on(ResumeEvent.class)))
                .next()
                .map(GatewayLifecycleEvent::getClient)
                .flatMap(DiscordClient::getApplicationInfo)
                .map(ApplicationInfo::getOwnerId)
                .map(Snowflake::asLong)
                .doOnNext(ownerId -> LogUtils.info("Bot owner ID: %d", ownerId))
                .subscribe(OWNER_ID::set);

        clients.flatMap(DiscordClient::login)
                .blockLast();
    }

    /**
     * Triggered when all the guilds have been received from a client
     */
    public static void onFullyReadyEvent(DiscordClient client) {
        if (Shadbot.CONNECTED_SHARDS.incrementAndGet() == client.getConfig().getShardCount()) {
            LogUtils.info("Shadbot is connected to all guilds.");
            if (!Config.IS_SNAPSHOT) {
                Shadbot.botListStats = new BotListStats(client);
                LogUtils.info("Bot list stats scheduler started.");
            }
        }
    }

    /**
     * @return The time when this class was loaded
     */
    public static Instant getLaunchTime() {
        return Shadbot.LAUNCH_TIME;
    }

    /**
     * @return The ID of the owner
     */
    public static Snowflake getOwnerId() {
        return Snowflake.of(Shadbot.OWNER_ID.get());
    }

    /**
     * @return All the shards the bot is connected to
     */
    public static Map<Integer, Shard> getShards() {
        return Collections.unmodifiableMap(Shadbot.SHARDS);
    }

    public static DiscordClient getClient() {
        return Shadbot.SHARDS.values().stream().findAny().orElseThrow().getClient();
    }

    private static void save() {
        DatabaseManager.getInstance().save();
        PremiumManager.getInstance().save();
        LotteryManager.getInstance().save();
        StatsManager.getInstance().save();
    }

    public static Mono<Void> quit(ExitCode exitCode) {
        if (Shadbot.botListStats != null) {
            Shadbot.botListStats.stop();
        }

        return Flux.fromIterable(Shadbot.SHARDS.values())
                .map(Shard::getClient)
                .flatMap(DiscordClient::logout)
                .then(Mono.fromRunnable(() -> System.exit(exitCode.getValue())));
    }

}
