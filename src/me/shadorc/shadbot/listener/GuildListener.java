package me.shadorc.shadbot.listener;

import java.util.Optional;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import discord4j.core.object.entity.Guild;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.LogUtils;

public class GuildListener {

	public static class GuildCreateListener implements Consumer<GuildCreateEvent> {
		@Override
		public void accept(GuildCreateEvent event) {
			ShardManager.execute(event.getGuild(), () -> {
				LogUtils.infof("Shadbot connected to a guild. (ID: %d | Users: %d)",
						event.getGuild().getId().asLong(), event.getGuild().getMemberCount().orElse(0));
			});
		}
	}

	public static class GuildDeleteListener implements Consumer<GuildDeleteEvent> {
		@Override
		public void accept(GuildDeleteEvent event) {
			Optional<Guild> guild = event.getGuild();
			ShardManager.execute(guild.orElse(null), () -> {
				LogUtils.infof("Shadbot disconnected from guild. (ID: %d | Users: %d)",
						event.getGuildId(), guild.isPresent() ? guild.get().getMemberCount().orElse(0) : -1);
			});
		}
	}
}
