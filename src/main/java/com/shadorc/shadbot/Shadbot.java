package com.shadorc.shadbot;

import com.fasterxml.jackson.databind.JavaType;
import com.rethinkdb.RethinkDB;
import com.rethinkdb.model.MapObject;
import com.rethinkdb.net.Connection;
import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.command.game.LotteryCmd;
import com.shadorc.shadbot.core.shard.Shard;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.db.guild.DBGuild;
import com.shadorc.shadbot.db.guild.DBMember;
import com.shadorc.shadbot.db.guild.GuildManager;
import com.shadorc.shadbot.db.lottery.LotteryManager;
import com.shadorc.shadbot.db.premium.PremiumManager;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.Utils;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
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

import java.io.File;
import java.net.ConnectException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Shadbot {

    private static final Instant LAUNCH_TIME = Instant.now();
    private static final AtomicLong OWNER_ID = new AtomicLong();
    private static final AtomicInteger CONNECTED_SHARDS = new AtomicInteger();
    private static final Map<Integer, Shard> SHARDS = new ConcurrentHashMap<>();

    private static BotListStats botListStats;

    public static void main(String[] args) {
        LogUtils.info("Starting Shadbot V%s", Config.VERSION);

        /*
        createDatabase();
        migrateGuild();
        migratePremium();
        migrateLottery();
         */

        LogUtils.info("Connecting to database...");
        Shadbot.connectToDatabase();

        // Set default to Locale US
        Locale.setDefault(Locale.US);

        LogUtils.info("Next lottery draw in: %s", LotteryCmd.getDelay().toString());
        Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7), Schedulers.elastic())
                .flatMap(ignored -> LotteryCmd.draw(Shadbot.getClient()))
                .onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));

        LogUtils.info("Connecting to Discord...");
        new ShardingClientBuilder(Credentials.get(Credential.DISCORD_TOKEN))
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
                .doOnNext(client -> {
                    final int shardIndex = client.getConfig().getShardIndex();
                    Shadbot.SHARDS.put(shardIndex, new Shard(client));

                    // Store bot owner ID
                    if (shardIndex == 0) {
                        client.getApplicationInfo()
                                .map(ApplicationInfo::getOwnerId)
                                .map(Snowflake::asLong)
                                .doOnNext(ownerId -> LogUtils.info("Bot owner ID: %d", ownerId))
                                .subscribe(Shadbot.OWNER_ID::set, err -> ExceptionHandler.handleUnknownError(client, err));
                    }
                })
                .flatMap(DiscordClient::login)
                .blockLast();
    }

    // TODO: Remove once migrated
    private static void createDatabase() {
        LogUtils.info("Creating database...");
        final RethinkDB db = GuildManager.getInstance().getDatabase();
        final Connection conn = GuildManager.getInstance().getConnection();
        db.dbCreate("shadbot").run(conn);
        db.db("shadbot").tableCreate("guild").run(conn);
        db.db("shadbot").tableCreate("premium").run(conn);
        db.db("shadbot").tableCreate("lottery").run(conn);
        LogUtils.info("Database created.");
    }

    private static void migrateGuild() {
        try {
            final RethinkDB db = GuildManager.getInstance().getDatabase();
            final Connection conn = GuildManager.getInstance().getConnection();

            LogUtils.info("Connected to %s:%d", conn.hostname, conn.port);

            final JavaType valueType = Utils.MAPPER.getTypeFactory().constructCollectionType(List.class, DBGuild.class);
            final List<DBGuild> guilds = Utils.MAPPER.readValue(new File("./saves/database.json"), valueType);
            for (final DBGuild guild : guilds) {
                if (guild.getMembers().isEmpty() && guild.settings.isEmpty()) {
                    continue;
                }

                LogUtils.info("Migrating guild: %s", guild.getId().asString());
                db.table("guild").insert(db.hashMap("id", guild.getId().asLong())).run(conn);

                final List<DBMember> membersFiltered = guild.getMembers().stream()
                        .filter(dbMember -> dbMember.getCoins() > 0)
                        .collect(Collectors.toList());
                if (!membersFiltered.isEmpty()) {
                    final List<MapObject> members = db.array();
                    for (final DBMember member : membersFiltered) {
                        members.add(db.hashMap("id", member.getId().asLong())
                                .with("coins", member.getCoins()));
                    }
                    db.table("guild").get(guild.getId().asLong()).update(
                            db.hashMap("members", members))
                            .run(conn);
                }

                if (!guild.settings.isEmpty()) {
                    MapObject settings = db.hashMap();
                    for (final Map.Entry<String, Object> setting : guild.settings.entrySet()) {
                        settings = settings.with(setting.getKey(), setting.getValue());
                    }
                    db.table("guild").get(guild.getId().asLong()).update(
                            db.hashMap("settings", settings))
                            .run(conn);
                }
            }

            LogUtils.info("Guild migration done.");
        } catch (final Exception err) {
            LogUtils.error(err, "An error occurred while migrating guilds.");
        }
    }

    private static void migratePremium() {

    }

    private static void migrateLottery() {

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

    private static void connectToDatabase() {
        try {
            GuildManager.getInstance().connect();
            PremiumManager.getInstance().connect();
            LotteryManager.getInstance().connect();
        } catch (final Exception err) {
            if (err.getCause() != null && err.getCause() instanceof ConnectException) {
                LogUtils.error("Database not connected. Exiting.");
            } else {
                LogUtils.error(err, "An error occurred while connecting to database. Exiting.");
            }
            Shadbot.quit(ExitCode.FATAL_ERROR).block();
        }
    }

    public static Mono<Void> quit(ExitCode exitCode) {
        if (Shadbot.botListStats != null) {
            Shadbot.botListStats.stop();
        }

        GuildManager.getInstance().stop();
        PremiumManager.getInstance().stop();
        LotteryManager.getInstance().stop();

        return Flux.fromIterable(Shadbot.SHARDS.values())
                .map(Shard::getClient)
                .flatMap(DiscordClient::logout)
                .then(Mono.fromRunnable(() -> System.exit(exitCode.getValue())));
    }

}
