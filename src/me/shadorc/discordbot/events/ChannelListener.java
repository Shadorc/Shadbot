package me.shadorc.discordbot.events;

import java.util.List;

import org.json.JSONArray;

import me.shadorc.discordbot.data.DatabaseManager;
import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelEvent;

@SuppressWarnings("ucd")
public class ChannelListener {

	@EventSubscriber
	public void onChannelEvent(ChannelEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			long startTime = System.currentTimeMillis();
			if(event instanceof ChannelDeleteEvent) {
				this.onChannelDeleteEvent((ChannelDeleteEvent) event);
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			if(elapsedTime / 1000 > 10) {
				LogUtils.info("{DEBUG} ChannelListener | Long event detected !"
						+ "\nDuration: " + elapsedTime
						+ "\nEvent: " + event);
			}
		});
	}

	private void onChannelDeleteEvent(ChannelDeleteEvent event) {
		List<Long> allowedChannelsID = Utils.convertToList((JSONArray) DatabaseManager.getSetting(event.getGuild(), Setting.ALLOWED_CHANNELS), Long.class);
		if(allowedChannelsID.remove(event.getChannel().getLongID())) {
			DatabaseManager.setSetting(event.getGuild(), Setting.ALLOWED_CHANNELS, new JSONArray(allowedChannelsID));
		}
	}
}
