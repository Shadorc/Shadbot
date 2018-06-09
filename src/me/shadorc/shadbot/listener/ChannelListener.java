package me.shadorc.shadbot.listener;

import java.util.List;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import twitter4j.JSONArray;

public class ChannelListener {

	public static void onTextChannelDelete(TextChannelDeleteEvent event) {
		DBGuild dbGuild = Database.getDBGuild(event.getChannel().getGuildId());
		List<Snowflake> allowedChannelIds = dbGuild.getAllowedChannels();
		// If the channel was an allowed channel...
		if(allowedChannelIds.remove(event.getChannel().getId())) {
			// ...update settings to remove the deleted one
			dbGuild.setSetting(SettingEnum.ALLOWED_CHANNELS, new JSONArray(allowedChannelIds));
		}
	}

}
