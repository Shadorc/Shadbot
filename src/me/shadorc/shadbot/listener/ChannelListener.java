package me.shadorc.shadbot.listener;

import java.util.List;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.setting.SettingEnum;
import me.shadorc.shadbot.data.database.DBGuild;
import reactor.core.publisher.Mono;

public class ChannelListener {

	/**
	 * Remove deleted text channels from allowed text channels setting.
	 *
	 * @param event - the event
	 */
	public static Mono<Void> onTextChannelDelete(TextChannelDeleteEvent event) {
		return Mono.fromRunnable(() -> {
			final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getChannel().getGuildId());
			final List<Snowflake> allowedChannelIds = dbGuild.getAllowedTextChannels();
			// If the channel was an allowed channel...
			if(allowedChannelIds.remove(event.getChannel().getId())) {
				// ...update settings to remove the deleted one
				dbGuild.setSetting(SettingEnum.ALLOWED_TEXT_CHANNELS, allowedChannelIds);
			}
		});
	}

}
