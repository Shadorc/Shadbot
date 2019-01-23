package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.SimpleBucket;
import discord4j.rest.request.SingleRouterFactory;
import me.shadorc.shadbot.command.game.LotteryCmd;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.core.shard.Shard;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.data.lottery.Lottery;
import me.shadorc.shadbot.data.lottery.LotteryManager;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.store.ShardJdkStoreService;
import me.shadorc.shadbot.store.ShardStoreRegistry;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Shadbot {

	private static final AtomicInteger CONNECTED_SHARDS = new AtomicInteger(0);
	private static final Instant LAUNCH_TIME = Instant.now();
	private static final Map<Integer, Shard> SHARDS = new ConcurrentHashMap<>();

	private static DatabaseManager databaseManager;
	private static PremiumManager premiumManager;
	private static LotteryManager lotteryManager;
	private static StatsManager statsManager;
	private static BotListStats botListStats;

	public static void main(String[] args) {
		// Set default to Locale US
		Locale.setDefault(Locale.US);

		try {
			Shadbot.databaseManager = new DatabaseManager();
			Shadbot.premiumManager = new PremiumManager();
			Shadbot.lotteryManager = new LotteryManager();
			Shadbot.statsManager = new StatsManager();
		} catch (final IOException err) {
			LogUtils.error(err, "A fatal error occurred while initializing managers.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		// If command generation has failed, abort attempt to connect the bot
		if(!CommandInitializer.init()) {
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		Runtime.getRuntime().addShutdownHook(new Thread(Shadbot::save));

		final ShardStoreRegistry registry = new ShardStoreRegistry();
		final DiscordClientBuilder builder = new DiscordClientBuilder(Credentials.get(Credential.DISCORD_TOKEN))
				.setEventScheduler(Schedulers.elastic())
				.setRouterFactory(new SingleRouterFactory())
				.setGatewayLimiter(new SimpleBucket(1, Duration.ofSeconds(6)))
				.setShardCount(DiscordUtils.getRecommendedShardCount(Credentials.get(Credential.DISCORD_TOKEN)).block())
				.setInitialPresence(Presence.idle(Activity.playing("Connecting...")));

		LogUtils.info("Connecting to %s...", StringUtils.pluralOf(builder.getShardCount(), "shard"));
		for(int index = 0; index < builder.getShardCount(); index++) {
			final DiscordClient client = builder.setShardIndex(index)
					.setStoreService(new ShardJdkStoreService(registry))
					.build();
			SHARDS.put(index, new Shard(client));
		}

		// Initiate login and block
		Mono.when(Shadbot.SHARDS.values()
				.stream()
				.map(Shard::getClient)
				.map(DiscordClient::login)
				.collect(Collectors.toList()))
				.block();
	}

	/**
	 * Triggered when all the guilds have been received from a client
	 */
	public static Mono<Void> onFullyReadyEvent(DiscordClient client) {
		return Mono.fromRunnable(() -> LogUtils.info("{Shard %d} Fully ready.", client.getConfig().getShardIndex()))
				.thenReturn(CONNECTED_SHARDS.incrementAndGet())
				.filter(connectedShards -> connectedShards == client.getConfig().getShardCount())
				.flatMap(ignored -> Shadbot.onFullyConnected());
	}

	/**
	 * Triggered when all the guilds have been received on all clients
	 */
	private static Mono<Void> onFullyConnected() {
		return Mono.fromRunnable(() -> {
			LogUtils.info("Shadbot is connected to all guilds.");
			Shadbot.botListStats = new BotListStats(SHARDS.values().stream().map(Shard::getClient).collect(Collectors.toList()));
		})
				.and(Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7))
						.doOnNext(ignored -> LotteryCmd.draw(Shadbot.getClient()))
						.onErrorContinue((err, obj) -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err)));
	}

	/**
	 * @return The time when this class was loaded
	 */
	public static Instant getLaunchTime() {
		return LAUNCH_TIME;
	}

	/**
	 * @return All the shards the bot is connected to
	 */
	public static Map<Integer, Shard> getShards() {
		return SHARDS;
	}

	public static DiscordClient getClient() {
		return SHARDS.values().stream().findAny().orElseThrow().getClient();
	}

	public static DatabaseManager getDatabase() {
		return databaseManager;
	}

	public static PremiumManager getPremium() {
		return premiumManager;
	}

	public static Lottery getLottery() {
		return lotteryManager.getLottery();
	}

	private static void save() {
		databaseManager.save();
		premiumManager.save();
		lotteryManager.save();
		statsManager.save();
	}

	private static void logout() {
		if(botListStats != null) {
			botListStats.stop();
		}
		SHARDS.values().stream().map(Shard::getClient).forEach(DiscordClient::logout);
	}

	public static void restart() {
		Shadbot.logout();
		System.exit(ExitCode.RESTART.value());
	}

	public static void quit() {
		Shadbot.logout();
		System.exit(ExitCode.NORMAL.value());
	}

}
