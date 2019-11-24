package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.core.object.util.Snowflake;
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
            final DBGuild dbGuild = DatabaseManager.getGuilds().getDBGuild(event.getChannel().getGuildId());
            final List<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
            // If the channel was an allowed channel...
            if (allowedTextChannelIds.remove(event.getChannel().getId())) {
                // ...update settings to remove the deleted one
                dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
            }
        });
    }

}
