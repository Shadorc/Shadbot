package com.shadorc.shadbot.data;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Summary;

public class Telemetry {
    
    private static final String PROCESS_NAMESPACE = "process";
    private static final String SHARD_NAMESPACE = "shard";
    private static final String SHADBOT_NAMESPACE = "shadbot";
    private static final String DATABASE_NAMESPACE = "database";
    private static final String DISCORD_NAMESPACE = "discord";
    private static final String MUSIC_NAMESPACE = "music";
    private static final String GAME_NAMESPACE = "game";

    public static final Gauge RAM_USAGE_GAUGE = Gauge.build("ram_usage_mb", "Ram usage in MB")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge CPU_USAGE_GAUGE = Gauge.build("cpu_usage_percent", "CPU usage in percent")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge THREAD_COUNT_GAUGE = Gauge.build("thread_count", "Thread count")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge GC_COUNT_GAUGE = Gauge.build("gc_count", "Garbage collector count")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge GC_TIME_GAUGE = Gauge.build("gc_time", "Garbage collector total time in ms")
            .namespace(PROCESS_NAMESPACE).register();
    public static final Gauge RESPONSE_TIME_GAUGE = Gauge.build("response_time", "Shard response time")
            .namespace(SHARD_NAMESPACE).labelNames("shard_id").register();
    public static final Gauge GUILD_COUNT_GAUGE = Gauge.build("guild_count", "Guild count")
            .namespace(SHADBOT_NAMESPACE).register();
    public static final Gauge VOICE_COUNT_GAUGE = Gauge.build("voice_count", "Connected voice channel count")
            .namespace(SHADBOT_NAMESPACE).register();

    public static final Counter EVENT_COUNTER = Counter.build("event_count", "Discord event count")
            .namespace(DISCORD_NAMESPACE).labelNames("type").register();
    public static final Counter MUSIC_ERROR_COUNTER = Counter.build("error_count", "Music error count")
            .namespace(MUSIC_NAMESPACE).labelNames("type").register();
    public static final Counter COMMAND_USAGE_COUNTER = Counter.build("command_usage", "Command usage")
            .namespace(SHADBOT_NAMESPACE).labelNames("command").register();
    public static final Counter REST_REQUEST_COUNTER = Counter.build("rest_request", "Rest request count")
            .namespace(SHARD_NAMESPACE).register();
    public static final Counter DB_REQUEST_COUNTER = Counter.build("request_count", "Database request count")
            .namespace(DATABASE_NAMESPACE).labelNames("collection").register();

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
