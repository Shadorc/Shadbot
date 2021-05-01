package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.command.Setting;
import com.shadorc.shadbot.database.DatabaseManager;
import discord4j.common.util.Snowflake;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import reactor.core.publisher.Mono;

import java.util.Set;

public class TextChannelDeleteListener implements EventListener<TextChannelDeleteEvent> {

    @Override
    public Class<TextChannelDeleteEvent> getEventType() {
        return TextChannelDeleteEvent.class;
    }

    @Override
    public Mono<?> execute(TextChannelDeleteEvent event) {
        return DatabaseManager.getGuilds()
                .getDBGuild(event.getChannel().getGuildId())
                .flatMap(dbGuild -> {
                    final Set<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                    // If the channel was an allowed channel...
                    if (allowedTextChannelIds.remove(event.getChannel().getId())) {
                        // ...update settings to remove the deleted one
                        return dbGuild.updateSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
                    }
                    return Mono.empty();
                });
    }

}
