package com.shadorc.shadbot;

import com.shadorc.shadbot.api.BotListStats;
import com.shadorc.shadbot.command.game.LotteryCmd;
import com.shadorc.shadbot.core.shard.Shard;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.Credentials;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.bean.DBGuildBean;
import com.shadorc.shadbot.db.guilds.bean.DBMemberBean;
import com.shadorc.shadbot.db.guilds.bean.SettingsBean;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.utils.ExceptionHandler;
import com.shadorc.shadbot.utils.ExitCode;
import com.shadorc.shadbot.utils.LogUtils;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public final class Shadbot {

    private static final Instant LAUNCH_TIME = Instant.now();
    private static final AtomicLong OWNER_ID = new AtomicLong();
    private static final AtomicInteger CONNECTED_SHARDS = new AtomicInteger();
    private static final Map<Integer, Shard> SHARDS = new ConcurrentHashMap<>();

    private static BotListStats botListStats;

    public static void main(String[] args) {
        Shadbot.migrateGuilds();

        LogUtils.info("Starting Shadbot V%s", Config.VERSION);

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
    private static void migrateGuilds() {
        final File saveFile = new File("./saves/database.json");
        if (!saveFile.exists()) {
            return;
        }

        try {
            final JSONArray array = new JSONArray(new JSONTokener(new FileInputStream(saveFile)));
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                final JSONArray members = obj.optJSONArray("members");
                final JSONObject settings = obj.optJSONObject("settings");

                if (members == null && settings == null) {
                    continue;
                }

                final String id = String.valueOf(obj.getLong("id"));

                final List<DBMemberBean> memberBeans = new ArrayList<>();
                if (members != null) {
                    for (int o = 0; o < members.length(); o++) {
                        final JSONObject memberObj = members.getJSONObject(o);
                        final DBMemberBean memberBean = new DBMemberBean(String.valueOf(memberObj.getLong("id")), memberObj.getInt("coins"));
                        if (memberBean.getCoins() != 0) {
                            memberBeans.add(memberBean);
                        }
                    }
                }

                SettingsBean settingsBean = null;
                if (settings != null) {

                    final JSONObject iamMessagesObj = settings.optJSONObject("iam_message");
                    Map<String, String> iamMessages = null;
                    if (iamMessagesObj != null) {
                        iamMessages = iamMessagesObj.toMap()
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toString()));
                    }

                    settingsBean = new SettingsBean(toList(settings.optJSONArray("allowed_text_channels")),
                            toList(settings.optJSONArray("allowed_voice_channels")),
                            toList(settings.optJSONArray("allowed_roles")),
                            toList(settings.optJSONArray("auto_roles")),
                            toList(settings.optJSONArray("blacklist")),
                            settings.optInt("default_volume") == 0 ? null : settings.getInt("default_volume"),
                            iamMessages,
                            settings.optString("join_message"),
                            settings.optString("leave_message"),
                            settings.optLong("message_channel_id") == 0 ? null : String.valueOf(settings.getLong("message_channel_id")),
                            null,
                            settings.optString("prefix"));
                }

                final DBGuildBean guildBean = new DBGuildBean(id, memberBeans, settingsBean);
                new DBGuild(guildBean).insert();

                System.out.println(guildBean);
            }
        } catch (final Exception err) {
            throw new RuntimeException(err);
        }
    }

    // TODO: Remove once migrated
    @Nullable
    private static List<String> toList(JSONArray array) {
        if (array == null) {
            return null;
        }
        final List<String> list = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            list.add(array.get(i).toString());
        }
        return list;
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

    public static Mono<Void> quit(ExitCode exitCode) {
        if (Shadbot.botListStats != null) {
            Shadbot.botListStats.stop();
        }

        DatabaseManager.getInstance().close();

        return Flux.fromIterable(Shadbot.SHARDS.values())
                .map(Shard::getClient)
                .flatMap(DiscordClient::logout)
                .then(Mono.fromRunnable(() -> System.exit(exitCode.getValue())));
    }

}
