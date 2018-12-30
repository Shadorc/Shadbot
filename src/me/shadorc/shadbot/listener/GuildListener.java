package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class GuildListener {

	public static Mono<Void> onGuildCreate(GuildCreateEvent event) {
		return Mono.just(event.getGuild().getMemberCount().orElse(-1))
				.doOnNext(memberCount -> LogUtils.info("{Guild ID: %d} Connected (Users: %d).",
						event.getGuild().getId().asLong(), memberCount))
				.then();
	}

	public static Mono<Void> onGuildDelete(GuildDeleteEvent event) {
		return Mono.fromRunnable(() -> LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong()));
	}
}
