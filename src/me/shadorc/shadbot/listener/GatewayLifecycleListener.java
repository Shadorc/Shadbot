package me.shadorc.shadbot.listener;

import java.time.Duration;

import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class GatewayLifecycleListener {

	public static Mono<Void> onGatewayLifecycleEvent(GatewayLifecycleEvent event) {
		return Mono.fromRunnable(() -> {
			LogUtils.info("{Shard %d} %s",
					event.getClient().getConfig().getShardIndex(),
					// Add space before uppercase letters
					event.getClass().getSimpleName().replaceAll("([^_])([A-Z])", "$1 $2"));
		});
	}

	public static Mono<Void> onReadyEvent(ReadyEvent event) {
		return Flux.interval(Duration.ZERO, Duration.ofMinutes(30))
				.flatMap(ignored -> DiscordUtils.updatePresence(event.getClient()))
				.onErrorContinue((err, obj) -> LogUtils.error(err, "An unknown error occurred while updating presence."))
				.then();
	}

}
