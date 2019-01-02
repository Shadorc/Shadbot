package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class GuildListener {

	public static Mono<Void> onGuildCreate(GuildCreateEvent event) {
		final long guildId = event.getGuild().getId().asLong();
		final int memberCount = event.getGuild().getMemberCount().orElse(-1);
		return Mono.fromRunnable(() -> LogUtils.info("{Guild ID: %d} Connected (%d users).", guildId, memberCount));
	}

	public static Mono<Void> onGuildDelete(GuildDeleteEvent event) {
		return Mono.fromRunnable(() -> LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong()));
	}
}
