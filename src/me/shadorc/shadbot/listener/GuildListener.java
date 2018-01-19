package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;

public class GuildListener {

	@EventSubscriber
	public void onGuildEvent(GuildEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			if(event instanceof GuildCreateEvent) {
				this.onGuildCreateEvent((GuildCreateEvent) event);
			} else if(event instanceof GuildLeaveEvent) {
				this.onGuildLeaveEvent((GuildLeaveEvent) event);
			}
		});
	}

	private void onGuildCreateEvent(GuildCreateEvent event) {
		LogUtils.infof("Shadbot connected to a guild. (ID: %d | Users: %d)",
				event.getGuild().getLongID(), event.getGuild().getUsers().size());
	}

	private void onGuildLeaveEvent(GuildLeaveEvent event) {
		LogUtils.infof("Shadbot disconnected from guild. (ID: %d | Users: %d)",
				event.getGuild().getLongID(), event.getGuild().getUsers().size());
	}
}
