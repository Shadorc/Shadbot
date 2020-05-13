package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.core.setting.Setting;
import com.shadorc.shadbot.db.DatabaseManager;
import discord4j.core.event.domain.channel.TextChannelDeleteEvent;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

public class TextChannelDeleteListener implements EventListener<TextChannelDeleteEvent> {

    @Override
    public Class<TextChannelDeleteEvent> getEventType() {
        return TextChannelDeleteEvent.class;
    }

    @Override
    public Mono<Void> execute(TextChannelDeleteEvent event) {
        return DatabaseManager.getGuilds()
                .getDBGuild(event.getChannel().getGuildId())
                .flatMap(dbGuild -> {
                    final Set<Snowflake> allowedTextChannelIds = dbGuild.getSettings().getAllowedTextChannelIds();
                    // If the channel was an allowed channel...
                    if (allowedTextChannelIds.remove(event.getChannel().getId())) {
                        // ...update settings to remove the deleted one
                        return dbGuild.setSetting(Setting.ALLOWED_TEXT_CHANNELS, allowedTextChannelIds);
                    }
                    return Mono.empty();
                })
                .then();
    }

}
