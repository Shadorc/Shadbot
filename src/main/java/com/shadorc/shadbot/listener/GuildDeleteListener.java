package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.MusicManager;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class GuildDeleteListener implements EventListener<GuildDeleteEvent> {

    @Override
    public Class<GuildDeleteEvent> getEventType() {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<?> execute(GuildDeleteEvent event) {
        if (event.isUnavailable()) {
            return Mono.empty();
        }

        DEFAULT_LOGGER.info("{Guild ID: {}} Disconnected", event.getGuildId().asLong());

        final Mono<Void> destroyVoiceConnection = MusicManager.getInstance()
                .destroyConnection(event.getGuildId());

        final Mono<Void> deleteGuild = DatabaseManager.getGuilds()
                .getDBGuild(event.getGuildId())
                .flatMap(DBGuild::delete);

        return destroyVoiceConnection
                .and(deleteGuild);
    }

}
