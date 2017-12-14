package me.shadorc.discordbot.events;

import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.GuildEvent;
import sx.blah.discord.handle.impl.events.guild.GuildLeaveEvent;

@SuppressWarnings("ucd")
public class GuildListener {

	@EventSubscriber
	public void onGuildEvent(GuildEvent event) {
		ShardManager.getThreadPool(event.getGuild().getShard()).execute(() -> {
			if(event instanceof GuildCreateEvent) {
				this.onGuildCreateEvent((GuildCreateEvent) event);
			} else if(event instanceof GuildLeaveEvent) {
				this.onGuildLeaveEvent((GuildLeaveEvent) event);
			}
		});
	}

	private void onGuildCreateEvent(GuildCreateEvent event) {
		LogUtils.info("Shadbot connected to a guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}

	private void onGuildLeaveEvent(GuildLeaveEvent event) {
		LogUtils.info("Shadbot disconnected from guild."
				+ " (ID: " + event.getGuild().getLongID() + " | Users: " + event.getGuild().getUsers().size() + ")");
	}
}
