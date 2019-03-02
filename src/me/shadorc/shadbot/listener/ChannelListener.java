package me.shadorc.shadbot.listener;

import java.util.List;

import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.setting.Setting;
import me.shadorc.shadbot.data.database.DBGuild;
import reactor.core.publisher.Mono;

public class ChannelListener {

	public static Mono<Void> onTextChannelDelete(TextChannelDeleteEvent event) {
		return Mono.fromRunnable(() -> {
			final DBGuild dbGuild = Shadbot.getDatabase().getDBGuild(event.getChannel().getGuildId());
			final List<Long> allowedTextChannelIds = dbGuild.getAllowedTextChannels();
			// If the channel was an allowed channel...
			if(allowedTextChannelIds.remove(event.getChannel().getId().asLong())) {
				// ...update settings to remove the deleted one
				dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
			}
		});
	}

}
