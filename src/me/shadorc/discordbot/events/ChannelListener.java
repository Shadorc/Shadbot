package me.shadorc.discordbot.events;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;

import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelEvent;

@SuppressWarnings("ucd")
public class ChannelListener {

	private final ExecutorService executor = Executors.newCachedThreadPool();

	@EventSubscriber
	public void onChannelEvent(ChannelEvent event) {
		if(event instanceof ChannelDeleteEvent) {
			executor.execute(() -> this.onChannelDelete((ChannelDeleteEvent) event));
		}
	}

	private void onChannelDelete(ChannelDeleteEvent event) {
		List<Long> allowedChannelsID = Utils.convertToList((JSONArray) DatabaseManager.getSetting(event.getGuild(), Setting.ALLOWED_CHANNELS), Long.class);
		if(allowedChannelsID.remove(event.getChannel().getLongID())) {
			DatabaseManager.setSetting(event.getGuild(), Setting.ALLOWED_CHANNELS, new JSONArray(allowedChannelsID));
		}
	}
}
