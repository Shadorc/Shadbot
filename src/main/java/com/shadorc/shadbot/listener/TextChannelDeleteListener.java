package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.guild.DBGuild;
import com.shadorc.shadbot.db.guild.GuildManager;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public class TextChannelDeleteListener implements EventListener<TextChannelDeleteEvent> {

    @Override
    public Class<TextChannelDeleteEvent> getEventType() {
        return TextChannelDeleteEvent.class;
    }

    @Override
    public Mono<Void> execute(TextChannelDeleteEvent event) {
        return Mono.fromRunnable(() -> {
            final DBGuild dbGuild = GuildManager.getInstance().getDBGuild(event.getChannel().getGuildId());
            final List<Long> allowedTextChannelIds = dbGuild.getAllowedTextChannels();
            // If the channel was an allowed channel...
            if (allowedTextChannelIds.remove(event.getChannel().getId().asLong())) {
                // ...update settings to remove the deleted one
                dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
            }
        });
    }

}
