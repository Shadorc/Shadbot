package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.guild.MemberLeaveEvent;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.MessageUpdateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.gateway.SimpleBucket;
import discord4j.store.jdk.JdkStoreService;
import me.shadorc.shadbot.command.game.LotteryCmd;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.data.credential.Credential;
import me.shadorc.shadbot.data.credential.Credentials;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.data.lottery.Lottery;
import me.shadorc.shadbot.data.lottery.LotteryManager;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.data.stats.StatsManager;
import me.shadorc.shadbot.listener.ChannelListener;
import me.shadorc.shadbot.listener.GatewayLifecycleListener;
import me.shadorc.shadbot.listener.GuildListener;
import me.shadorc.shadbot.listener.MemberListener;
import me.shadorc.shadbot.listener.MessageCreateListener;
import me.shadorc.shadbot.listener.MessageUpdateListener;
import me.shadorc.shadbot.listener.ReactionListener;
import me.shadorc.shadbot.listener.VoiceStateUpdateListener;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class Shadbot {

	private static final AtomicInteger CONNECTED_SHARDS = new AtomicInteger(0);
	private static final int SHARD_COUNT = 11;
	private static final Instant LAUNCH_TIME = Instant.now();
	private static final List<DiscordClient> CLIENTS = new ArrayList<>();

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

		final JdkStoreService sharedStore = new JdkStoreService();
		final DiscordClientBuilder builder = new DiscordClientBuilder(Credentials.get(Credential.DISCORD_TOKEN))
				.setEventScheduler(Schedulers.elastic())
				.setStoreService(sharedStore)
				.setGatewayLimiter(new SimpleBucket(1, Duration.ofSeconds(6)))
				.setShardCount(SHARD_COUNT)
				.setInitialPresence(Presence.idle(Activity.playing("Connecting...")));

		LogUtils.info("Connecting to %s...", StringUtils.pluralOf(builder.getShardCount(), "shard"));
		for(int index = 0; index < builder.getShardCount(); index++) {
			final DiscordClient client = builder.setShardIndex(index).build();
			CLIENTS.add(client);

			DiscordUtils.register(client, ReadyEvent.class, ReadyListener::onReadyEvent);
			DiscordUtils.register(client, TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
			DiscordUtils.register(client, GuildDeleteEvent.class, GuildListener::onGuildDelete);
			DiscordUtils.register(client, MemberJoinEvent.class, MemberListener::onMemberJoin);
			DiscordUtils.register(client, MemberLeaveEvent.class, MemberListener::onMemberLeave);
			DiscordUtils.register(client, MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
			DiscordUtils.register(client, MessageUpdateEvent.class, MessageUpdateListener::onMessageUpdateEvent);
			DiscordUtils.register(client, VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);
			DiscordUtils.register(client, ReactionAddEvent.class, ReactionListener::onReactionAddEvent);
			DiscordUtils.register(client, ReactionRemoveEvent.class, ReactionListener::onReactionRemoveEvent);
			DiscordUtils.registerFullyReadyEvent(client, Shadbot::onFullyReadyEvent);
		}

		// Initiate login and block
		Mono.when(Shadbot.CLIENTS.stream().map(DiscordClient::login).collect(Collectors.toList())).block();
	}

	/**
	 * Triggered when all the guilds have been received from a client
	 */
	private static Mono<Void> onFullyReadyEvent(GuildCreateEvent event) {
		return Mono.fromRunnable(() -> {
			CONNECTED_SHARDS.incrementAndGet();

			LogUtils.info("{Shard %d} Fully ready. %s left...",
					event.getClient().getConfig().getShardIndex(), StringUtils.pluralOf(SHARD_COUNT - CONNECTED_SHARDS.get(), "shard"));

			DiscordUtils.register(event.getClient(), GuildCreateEvent.class, GuildListener::onGuildCreate);
			DiscordUtils.register(event.getClient(), GatewayLifecycleEvent.class, GatewayLifecycleListener::onGatewayLifecycleEvent);
		})
				.then(CONNECTED_SHARDS.get() == SHARD_COUNT ? Shadbot.onFullyConnected() : Mono.empty());
	}

	/**
	 * Triggered when all the guilds have been received on all clients
	 */
	private static Mono<Void> onFullyConnected() {
		return Mono.fromRunnable(() -> {
			LogUtils.info("Shadbot is connected to all guilds.");
			Shadbot.botListStats = new BotListStats(CLIENTS.get(0).getSelfId().get());
		})
				.thenMany(Flux.interval(LotteryCmd.getDelay(), Duration.ofDays(7)))
				.doOnNext(ignored -> LotteryCmd.draw(CLIENTS.get(0)))
				.onErrorContinue((err, obj) -> LogUtils.error(err, "An unknown error occurred during the lottery draw."))
				.then();
	}

	/**
	 * @return The time when this class was loaded
	 */
	public static Instant getLaunchTime() {
		return LAUNCH_TIME;
	}

	/**
	 * @return All the clients the bot is connected to
	 */
	public static List<DiscordClient> getClients() {
		return CLIENTS;
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
		CLIENTS.forEach(DiscordClient::logout);
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
