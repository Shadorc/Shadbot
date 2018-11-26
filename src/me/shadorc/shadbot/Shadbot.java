package me.shadorc.shadbot;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.Event;
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
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.data.apikey.APIKey;
import me.shadorc.shadbot.data.apikey.APIKeys;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.data.lotto.Lotto;
import me.shadorc.shadbot.data.lotto.LottoManager;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.ChannelListener;
import me.shadorc.shadbot.listener.GatewayLifecycleListener;
import me.shadorc.shadbot.listener.GuildListener;
import me.shadorc.shadbot.listener.MemberListener;
import me.shadorc.shadbot.listener.MessageCreateListener;
import me.shadorc.shadbot.listener.MessageUpdateListener;
import me.shadorc.shadbot.listener.ReactionListener;
import me.shadorc.shadbot.utils.ExitCode;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

public class Shadbot {

	private static final Instant LAUNCH_TIME = Instant.now();
	private static final List<DiscordClient> CLIENTS = new ArrayList<>();

	private static PremiumManager premiumManager;
	private static DatabaseManager databaseManager;
	private static LottoManager lottoManager;
	private static APIKeys apiKeysManager;
	private static BotListStats botListStats;

	public static void main(String[] args) {
		// Enable full Reactor stack-traces
		Hooks.onOperatorDebug();
		// Set default to Locale US
		Locale.setDefault(Locale.US);

		try {
			Shadbot.premiumManager = new PremiumManager();
			Shadbot.databaseManager = new DatabaseManager();
			Shadbot.lottoManager = new LottoManager();
			Shadbot.apiKeysManager = new APIKeys();
			Shadbot.botListStats = new BotListStats();
		} catch (IOException err) {
			LogUtils.error(err, "A fatal error occurred while initializing managers.");
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		// If command generation has failed, abort attempt to connect the bot
		if(!CommandInitializer.init()) {
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		Runtime.getRuntime().addShutdownHook(new Thread(Shadbot::save));

		final int shardCount = 1;
		final DiscordClientBuilder builder = new DiscordClientBuilder(APIKeys.get(APIKey.DISCORD_TOKEN))
				.setGatewayLimiter(new SimpleBucket(1, Duration.ofSeconds(6)))
				.setShardCount(shardCount)
				.setInitialPresence(Presence.idle(Activity.playing("Connecting...")));

		LogUtils.info("Connecting to %s...", StringUtils.pluralOf(builder.getShardCount(), "shard"));
		for(int index = 0; index < builder.getShardCount(); index++) {
			final DiscordClient client = builder.setShardIndex(index).build();
			CLIENTS.add(client);

			Shadbot.register(client, ReadyEvent.class, GatewayLifecycleListener::onReadyEvent);
			Shadbot.register(client, TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
			Shadbot.register(client, GuildDeleteEvent.class, GuildListener::onGuildDelete);
			Shadbot.register(client, MemberJoinEvent.class, MemberListener::onMemberJoin);
			Shadbot.register(client, MemberLeaveEvent.class, MemberListener::onMemberLeave);
			Shadbot.register(client, MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
			Shadbot.register(client, MessageUpdateEvent.class, MessageUpdateListener::onMessageUpdateEvent);
			// TODO: Implement
			// Shadbot.register(client, VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);
			Shadbot.register(client, ReactionAddEvent.class, ReactionListener::onReactionAddEvent);
			Shadbot.register(client, ReactionRemoveEvent.class, ReactionListener::onReactionRemoveEvent);
		
			// When all the guilds have been received, register GuildListener#onGuildCreate and GatewayLifecycleListener#onGatewayLifecycleEvent
			client.getEventDispatcher().on(ReadyEvent.class)
			        .map(event -> event.getGuilds().size())
			        .flatMap(size -> client.getEventDispatcher()
			        		.on(GuildCreateEvent.class)
			        		.take(size)
			        		.last())
			        .subscribe(event -> {
			        	LogUtils.info("{Shard %d} Fully connected to Gateway.", client.getConfig().getShardIndex());
		        		Shadbot.register(client, GuildCreateEvent.class, GuildListener::onGuildCreate);
		        		Shadbot.register(client, GatewayLifecycleEvent.class, GatewayLifecycleListener::onGatewayLifecycleEvent);
			        });
		}

		Flux.interval(LottoCmd.getDelay(), Duration.ofDays(7))
				.doOnNext(ignored -> LottoCmd.draw(CLIENTS.get(0)))
				.subscribe();
		
		// Initiate login and block
		Mono.when(Shadbot.CLIENTS.stream().map(DiscordClient::login).collect(Collectors.toList())).block();
	}

	/**
	 * @return The time when this class was loaded.
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

	public static PremiumManager getPremium() {
		return premiumManager;
	}

	public static DatabaseManager getDatabase() {
		return databaseManager;
	}

	public static Lotto getLotto() {
		return lottoManager.getLotto();
	}

	public static APIKeys getAPIKeys() {
		return apiKeysManager;
	}

	private static <T extends Event> void register(DiscordClient client, Class<T> eventClass, Consumer<? super T> consumer) {
		client.getEventDispatcher()
				.on(eventClass)
				.onErrorContinue((err, obj) -> LogUtils.error(client, err, String.format("An unknown error occurred on %s.", eventClass.getSimpleName())))
				.subscribe(consumer);
	}

	private static void save() {
		premiumManager.save();
		databaseManager.save();
		lottoManager.save();
	}

	private static void logout() {
		botListStats.stop();
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
