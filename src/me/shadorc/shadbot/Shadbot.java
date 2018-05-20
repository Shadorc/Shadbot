package me.shadorc.shadbot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import discord4j.core.ClientBuilder;
import discord4j.core.DiscordClient;
import discord4j.core.event.domain.Event;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.listener.GatewayLifecycleListener;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.executor.ScheduledWrappedExecutor;
import reactor.core.publisher.Mono;

public class Shadbot {

	private static final ScheduledThreadPoolExecutor SCHEDULER = new ScheduledWrappedExecutor(3, "ShadbotScheduler-%d");

	private static List<DiscordClient> clients;

	public static void main(String[] args) {
		Locale.setDefault(new Locale("en", "US"));

		// If file loading or command generation has failed, abort attempt to connect the bot
		if(!DataManager.init() || !CommandManager.init()) {
			System.exit(1);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				DataManager.stop();
			}
		});

		// TODO: Calculate this by using gateway or guilds / 1000
		int shardCount = 7;

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(shardCount, "shard"));

		List<Mono<Void>> logins = new ArrayList<>();
		for(int i = 0; i < shardCount; i++) {
			DiscordClient client = new ClientBuilder(APIKeys.get(APIKey.DISCORD_TOKEN))
					.setInitialPresence(Presence.idle(Activity.playing("Connecting...")))
					.setShardIndex(i)
					.setShardCount(shardCount)
					.build();
			clients.add(client);

			Shadbot.registerListener(client, GatewayLifecycleEvent.class, GatewayLifecycleListener::onGatewayLifecycleEvent);

			logins.add(client.login());
		}

		Shadbot.scheduleAtFixedRate(LottoCmd::draw, LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);

		logins.stream().forEach(Mono::block);
	}

	// TODO: Find an alternative ?
	public static DiscordClient getClient() {
		return clients.get(0);
	}

	public static <T extends Event> void registerListener(DiscordClient client, Class<T> eventClass, Consumer<? super T> consumer) {
		client.getEventDispatcher().on(eventClass)
				.doOnError(err -> LogUtils.error(err, String.format("An unknown error occurred on %s.", eventClass.getSimpleName())))
				.subscribe(consumer);
	}

	public static void scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		SCHEDULER.scheduleAtFixedRate(command, initialDelay, period, unit);
	}

}
