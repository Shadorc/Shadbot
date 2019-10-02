package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.database.DatabaseManager;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import reactor.core.publisher.Mono;

public class GuildDeleteListener implements EventListener<GuildDeleteEvent> {

    @Override
    public Class<GuildDeleteEvent> getEventType() {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<Void> execute(GuildDeleteEvent event) {
        return Mono.fromRunnable(() -> {
            LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong());
            MusicManager.getInstance().removeConnection(event.getGuildId());
            DatabaseManager.getInstance().deleteDBGuild(event.getGuildId());
        });
    }

}
