package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
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
        LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong());
        return MusicManager.getInstance()
                .destroyConnection(event.getGuildId())
                .then(DatabaseManager.getGuilds()
                        .getDBGuild(event.getGuildId()))
                .flatMap(DBGuild::delete);
    }

}
