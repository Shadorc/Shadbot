package me.shadorc.shadbot.listener;

import java.util.List;

import org.json.JSONArray;

import me.shadorc.shadbot.data.Database;
import me.shadorc.shadbot.data.Setting;
import me.shadorc.shadbot.shard.ShardManager;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.channel.ChannelEvent;

public class ChannelListener {

	@EventSubscriber
	public void onChannelEvent(ChannelEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			if(event instanceof ChannelDeleteEvent) {
				this.onChannelDeleteEvent((ChannelDeleteEvent) event);
			}
		});
	}

	private void onChannelDeleteEvent(ChannelDeleteEvent event) {
		List<Long> allowedChannelsID = Database.getDBGuild(event.getGuild()).getAllowedChannels();
		if(allowedChannelsID.remove(event.getChannel().getLongID())) {
			Database.getDBGuild(event.getGuild()).setSetting(Setting.ALLOWED_CHANNELS, new JSONArray(allowedChannelsID));
		}
	}
}
