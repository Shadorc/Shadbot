package me.shadorc.shadbot.listener;

import java.time.Duration;

import discord4j.core.DiscordClient;
import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;

public class GatewayLifecycleListener {

	public static void onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		LogUtils.infof("{Shard %d} %s",
				event.getClient().getConfig().getShardIndex(),
				event.toString());
	}

	public static void onReadyEvent(ReadyEvent event) {
		final DiscordClient client = event.getClient();

		Flux.interval(Duration.ofHours(2), Duration.ofHours(2))
				.flatMap(ignored -> NetUtils.postStats(client))
				.doOnError(err -> LogUtils.error(client, err, "An error occurred while posting statistics."))
				.subscribe();

		Flux.interval(Duration.ZERO, Duration.ofMinutes(30))
				.flatMap(ignored -> BotUtils.updatePresence(client))
				.doOnError(err -> LogUtils.error(client, err, "An error occurred while updating presence."))
				.subscribe();
	}

}