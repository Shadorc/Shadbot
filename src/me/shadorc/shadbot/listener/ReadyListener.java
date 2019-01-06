package me.shadorc.shadbot.listener;

import java.time.Duration;

import discord4j.core.event.domain.lifecycle.GatewayLifecycleEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReadyListener {

	public static Mono<Void> onReadyEvent(ReadyEvent event) {
		return Flux.interval(Duration.ZERO, Duration.ofMinutes(30))
				.flatMap(ignored -> DiscordUtils.updatePresence(event.getClient()))
				.onErrorContinue((err, obj) -> LogUtils.error(err, "An unknown error occurred while updating presence."))
				.then();
	}

}
