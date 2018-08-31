package me.shadorc.shadbot.listener;

import java.util.Optional;
import java.util.OptionalInt;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.entity.Guild;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class GuildListener {

	public static void onGuildCreate(GuildCreateEvent event) {
		LogUtils.infof("Shadbot connected to a guild. (ID: %d | Users: %d)",
				event.getGuild().getId().asLong(),
				event.getGuild().getMemberCount().orElse(-1));
	}

	public static void onGuildDelete(GuildDeleteEvent event) {
		final Optional<Guild> guild = event.getGuild();
		LogUtils.infof("Shadbot disconnected from a guild. (ID: %d | Users: %d)",
				event.getGuildId().asLong(),
				guild.map(Guild::getMemberCount).orElse(OptionalInt.of(-1)).getAsInt());
	}
}
