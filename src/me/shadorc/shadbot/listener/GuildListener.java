package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GuildListener {

	public static void onGuildCreate(GuildCreateEvent event) {
		if(Shadbot.isReady()) {
			LogUtils.info("Shadbot connected to a guild. (ID: %d | Users: %d)",
					event.getGuild().getId().asLong(),
					event.getGuild().getMemberCount().orElse(-1));
		}
	}

	public static void onGuildDelete(GuildDeleteEvent event) {
		LogUtils.info("Shadbot disconnected from a guild. (ID: %d)",
				event.getGuildId().asLong());
	}
}
