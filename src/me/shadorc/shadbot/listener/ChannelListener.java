package me.shadorc.shadbot.listener;

import java.util.List;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.entity.Guild;
import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import me.shadorc.shadbot.command.admin.setting.core.SettingEnum;
import me.shadorc.shadbot.data.db.DBGuild;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.shard.ShardManager;
import twitter4j.JSONArray;

public class ChannelListener {

	public static class TextChannelDeleteListener implements Consumer<TextChannelDeleteEvent> {
		@Override
		public void accept(TextChannelDeleteEvent event) {
			Guild guild = event.getChannel().getGuild().block();
			ShardManager.execute(guild, () -> {
				DBGuild dbGuild = Database.getDBGuild(guild);
				List<Long> allowedChannelsID = dbGuild.getAllowedChannels();
				if(allowedChannelsID.remove(event.getChannel().getId().asLong())) {
					dbGuild.setSetting(SettingEnum.ALLOWED_CHANNELS, new JSONArray(allowedChannelsID));
				}
			});
		}

	}

}
