package me.shadorc.discordbot.events;

import org.json.JSONArray;

import me.shadorc.discordbot.data.Setting;
import me.shadorc.discordbot.data.StorageManager;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;

@SuppressWarnings("ucd")
public class ChannelListener {

	@EventSubscriber
	public void onChannelDeleteEvent(ChannelDeleteEvent event) {
		JSONArray allowedChannelsArray = (JSONArray) StorageManager.getSetting(event.getGuild(), Setting.ALLOWED_CHANNELS);
		for(int i = 0; i < allowedChannelsArray.length(); i++) {
			if(allowedChannelsArray.getLong(i) == event.getChannel().getLongID()) {
				allowedChannelsArray.remove(i);
				break;
			}
		}

		StorageManager.setSetting(event.getGuild(), Setting.ALLOWED_CHANNELS, allowedChannelsArray);
	}
}
