package me.shadorc.shadbot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import me.shadorc.shadbot.command.game.LottoCmd;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.data.DataManager;
import me.shadorc.shadbot.listener.GatewayLifecycleListener;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.SchedulerUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;

public class Shadbot {

	private static final Instant LAUNCH_TIME = Instant.now();
	private static final List<DiscordClient> CLIENTS = new ArrayList<>();

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);

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

		final int shardCount = DiscordUtils.getRecommendedShardCount();

		LogUtils.infof("Connecting to %s...", StringUtils.pluralOf(shardCount, "shard"));

		for(int i = 0; i < shardCount; i++) {
			DiscordClient client = new DiscordClientBuilder(APIKeys.get(APIKey.DISCORD_TOKEN))
					.setInitialPresence(Presence.idle(Activity.playing("Connecting...")))
					.setShardIndex(i)
					.setShardCount(shardCount)
					.build();
			CLIENTS.add(client);

			DiscordUtils.registerListener(client, GatewayLifecycleEvent.class, GatewayLifecycleListener::onGatewayLifecycleEvent);
			DiscordUtils.registerListener(client, ReadyEvent.class, GatewayLifecycleListener::onReady);
		}

		SchedulerUtils.scheduleAtFixedRate(() -> LottoCmd.draw(CLIENTS.get(0)), LottoCmd.getDelay(), TimeUnit.DAYS.toMillis(7), TimeUnit.MILLISECONDS);

		// Initiate login and block
		Flux.merge(Flux.fromIterable(CLIENTS)).flatMap(DiscordClient::login).blockLast();
	}

	public static void logout() {
		CLIENTS.forEach(DiscordClient::logout);
		System.exit(0);
	}

	/**
	 * @return The time when this class was loaded.
	 */
	public static Instant getLaunchTime() {
		return LAUNCH_TIME;
	}

}
