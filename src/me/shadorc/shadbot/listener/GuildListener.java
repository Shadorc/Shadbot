package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GuildListener {

	public static void onGuildCreate(GuildCreateEvent event) {
		event.getGuild()
				.getMemberCount()
				.ifPresent(memberCount -> LogUtils.info("{Guild ID: %d} Connected (Users: %d).",
						event.getGuild().getId().asLong(), memberCount));
	}

	public static void onGuildDelete(GuildDeleteEvent event) {
		LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong());
	}
}
