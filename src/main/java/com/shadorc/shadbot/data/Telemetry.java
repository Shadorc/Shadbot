package com.shadorc.shadbot.data;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;

import java.util.HashSet;
import java.util.Set;

public class Telemetry {

    public static final Set<Long> INTERACTING_USERS = new HashSet<>();
    public static final Set<Long> GUILD_IDS = new HashSet<>();
    public static final Set<Long> CONNECTED_VOICE_CHANNEL_IDS = new HashSet<>();

    private static final String SYSTEM_NAMESPACE = "system";
    private static final String SHARD_NAMESPACE = "shard";
    private static final String SHADBOT_NAMESPACE = "shadbot";
    private static final String DATABASE_NAMESPACE = "database";
    private static final String DISCORD_NAMESPACE = "discord";
    private static final String MUSIC_NAMESPACE = "music";
    private static final String GAME_NAMESPACE = "game";
    private static final String PROCESS_NAMESPACE = "process";

    public static final Gauge UPTIME_GAUGE = Gauge.build("uptime", "Uptime in ms")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge PROCESS_CPU_USAGE_GAUGE = Gauge.build("process_cpu_usage", "Process CPU usage")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge SYSTEM_CPU_USAGE_GAUGE = Gauge.build("system_cpu_usage", "System CPU usage")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge MAX_HEAP_MEMORY_GAUGE = Gauge.build("max_heap_memory", "Max Heap memory")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge TOTAL_HEAP_MEMORY_GAUGE = Gauge.build("total_heap_memory", "Total Heap memory")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge USED_HEAP_MEMORY_GAUGE = Gauge.build("used_heap_memory", "Used Heap memory")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge SYSTEM_TOTAL_MEMORY_GAUGE = Gauge.build("total_memory", "Total memory")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge SYSTEM_FREE_MEMORY_GAUGE = Gauge.build("free_memory", "Free memory")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge GC_COUNT_GAUGE = Gauge.build("gc_count", "Garbage collector count")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge GC_TIME_GAUGE = Gauge.build("gc_time", "Garbage collector total time in ms")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge THREAD_COUNT_GAUGE = Gauge.build("thread_count", "Thread count")
            .namespace(SYSTEM_NAMESPACE).register();
    public static final Gauge DAEMON_THREAD_COUNT_GAUGE = Gauge.build("daemon_thread_count", "Daemon thread count")
            .namespace(SYSTEM_NAMESPACE).register();

    public static final Gauge GUILD_COUNT_GAUGE = Gauge.build("guild_count", "Guild count")
            .namespace(SHADBOT_NAMESPACE).register();
    public static final Gauge RESPONSE_TIME_GAUGE = Gauge.build("response_time", "Shard response time")
            .namespace(SHARD_NAMESPACE).labelNames("shard_id").register();
    public static final Gauge VOICE_COUNT_GAUGE = Gauge.build("voice_count", "Connected voice channel count")
            .namespace(SHADBOT_NAMESPACE).register();
    public static final Gauge UNIQUE_INTERACTING_USERS = Gauge.build("unique_interacting_users",
            "Unique interacting users count").namespace(SHADBOT_NAMESPACE).register();

    public static final Gauge PROCESS_TOTAL_MEMORY = Gauge.build("total_memory", "Total memory")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge PROCESS_FREE_MEMORY = Gauge.build("free_memory", "Free memory")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge PROCESS_MAX_MEMORY = Gauge.build("max_memory", "Max memory")
            .namespace(PROCESS_NAMESPACE).register();

    public static final Counter EVENT_COUNTER = Counter.build("event_count", "Discord event count")
            .namespace(DISCORD_NAMESPACE).labelNames("type").register();
    public static final Counter VOICE_CHANNEL_ERROR_COUNTER = Counter.build("voice_channel_error_count",
            "Voice channel error count").namespace(DISCORD_NAMESPACE).labelNames("exception").register();
    public static final Counter COMMAND_USAGE_COUNTER = Counter.build("command_usage", "Command usage")
            .namespace(SHADBOT_NAMESPACE).labelNames("command").register();
    public static final Counter REST_REQUEST_COUNTER = Counter.build("rest_request", "REST request count")
            .namespace(SHARD_NAMESPACE).labelNames("route").register();
    public static final Counter DB_REQUEST_COUNTER = Counter.build("request_count", "Database request count")
            .namespace(DATABASE_NAMESPACE).labelNames("collection").register();
    public static final Counter MESSAGE_SENT_COUNTER = Counter.build("message_sent", "Message sent count")
            .namespace(DISCORD_NAMESPACE).register();
    public static final Counter MUSIC_ERROR_COUNTER = Counter.build("music_error_count", "Music error count")
            .namespace(MUSIC_NAMESPACE).labelNames("exception").register();

    public static final Summary BLACKJACK_SUMMARY = Summary.build("blackjack", "Blackjack game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary DICE_SUMMARY = Summary.build("dice", "Dice game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary HANGMAN_SUMMARY = Summary.build("hangman", "Hangman game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary ROULETTE_SUMMARY = Summary.build("roulette", "Roulette game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary RPS_SUMMARY = Summary.build("rps", "RPS game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary RUSSIAN_ROULETTE_SUMMARY = Summary.build("russian_roulette", "Russian Roulette game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary TRIVIA_SUMMARY = Summary.build("trivia", "Trivia game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();
    public static final Summary SLOT_MACHINE_SUMMARY = Summary.build("slot_machine", "Slot Machine game")
            .namespace(GAME_NAMESPACE).labelNames("result").register();

}
