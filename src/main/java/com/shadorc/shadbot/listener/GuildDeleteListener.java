package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.MusicManager;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;
import static com.shadorc.shadbot.listener.GuildCreateListener.GUILD_COUNT_GAUGE;

public class GuildDeleteListener implements EventListener<GuildDeleteEvent> {

    @Override
    public Class<GuildDeleteEvent> getEventType() {
        return GuildDeleteEvent.class;
    }

    @Override
    public Mono<Void> execute(GuildDeleteEvent event) {
        DEFAULT_LOGGER.info("{Guild ID: {}} Disconnected", event.getGuildId().asLong());

        GUILD_COUNT_GAUGE.dec();

        return MusicManager.getInstance()
                .destroyConnection(event.getGuildId())
                .then(DatabaseManager.getGuilds()
                        .getDBGuild(event.getGuildId()))
                .flatMap(DBGuild::delete);
    }

}
