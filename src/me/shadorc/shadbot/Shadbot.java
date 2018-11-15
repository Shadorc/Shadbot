package me.shadorc.shadbot;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
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
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
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
	private static final AtomicBoolean IS_READY = new AtomicBoolean(false);

	public static void main(String[] args) {
		// Enable full Reactor stack-traces
		Hooks.onOperatorDebug();
		// Set default to Locale US
		Locale.setDefault(Locale.US);

		// If file loading or command generation has failed, abort attempt to connect the bot
		if(!DataManager.init() || !CommandInitializer.init()) {
			System.exit(ExitCode.FATAL_ERROR.value());
		}

		Runtime.getRuntime().addShutdownHook(new Thread(DataManager::stop));

		final int shardCount = 11;
		final DiscordClientBuilder builder = new DiscordClientBuilder(APIKeys.get(APIKey.DISCORD_TOKEN))
				.setGatewayLimiter(new SimpleBucket(1, Duration.ofSeconds(6)))
				.setShardCount(shardCount)
				.setInitialPresence(Presence.idle(Activity.playing("Connecting...")));

		LogUtils.info("Connecting to %s...", StringUtils.pluralOf(builder.getShardCount(), "shard"));
		for(int index = 0; index < builder.getShardCount(); index++) {
			final DiscordClient client = builder.setShardIndex(index).build();
			CLIENTS.add(client);

			Shadbot.register(client, GatewayLifecycleEvent.class, GatewayLifecycleListener::onGatewayLifecycleEvent);
			Shadbot.register(client, ReadyEvent.class, GatewayLifecycleListener::onReadyEvent);
			Shadbot.register(client, TextChannelDeleteEvent.class, ChannelListener::onTextChannelDelete);
			Shadbot.register(client, GuildCreateEvent.class, GuildListener::onGuildCreate);
			Shadbot.register(client, GuildDeleteEvent.class, GuildListener::onGuildDelete);
			Shadbot.register(client, MemberJoinEvent.class, MemberListener::onMemberJoin);
			Shadbot.register(client, MemberLeaveEvent.class, MemberListener::onMemberLeave);
			Shadbot.register(client, MessageCreateEvent.class, MessageCreateListener::onMessageCreate);
			Shadbot.register(client, MessageUpdateEvent.class, MessageUpdateListener::onMessageUpdateEvent);
			// TODO: Implement
			// Shadbot.register(client, VoiceStateUpdateEvent.class, VoiceStateUpdateListener::onVoiceStateUpdateEvent);
			Shadbot.register(client, ReactionAddEvent.class, ReactionListener::onReactionAddEvent);
			Shadbot.register(client, ReactionRemoveEvent.class, ReactionListener::onReactionRemoveEvent);
		}

		Flux.interval(LottoCmd.getDelay(), Duration.ofDays(7))
				.doOnNext(ignored -> LottoCmd.draw(CLIENTS.get(0)))
				.subscribe();

		// TODO: Find a better and more consistent solution
		Mono.delay(Duration.ofSeconds(shardCount * 8))
				.then(Mono.fromRunnable(() -> IS_READY.set(true)))
				.then(Mono.fromRunnable(() -> LogUtils.info("Shadbot (Version: %s) is ready", Config.VERSION)))
				.subscribe();

		// Initiate login and block
		Mono.when(CLIENTS.stream().map(DiscordClient::login).collect(Collectors.toList())).block();
	}

	private static void logout() {
		CLIENTS.forEach(DiscordClient::logout);
	}

	public static void quit() {
		Shadbot.logout();
		System.exit(ExitCode.NORMAL.value());
	}

	public static void restart() {
		Shadbot.logout();
		System.exit(ExitCode.RESTART.value());
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

	/**
	 * @return true when the bot is connected to all its guilds, false otherwise
	 */
	public static boolean isReady() {
		return IS_READY.get();
	}

	private static <T extends Event> void register(DiscordClient client, Class<T> eventClass, Consumer<? super T> consumer) {
		client.getEventDispatcher().on(eventClass)
				.onErrorContinue((err, obj) -> LogUtils.error(client, err, String.format("An unknown error occurred on %s.", eventClass.getSimpleName())))
				.subscribe(consumer);
	}

}
