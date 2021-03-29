package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.database.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.MusicManager;
import discord4j.common.util.Snowflake;
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

        final Snowflake guildId = event.getGuildId();
        DEFAULT_LOGGER.info("{Guild ID: {}} Disconnected", guildId.asString());

        final Mono<Void> destroyVoiceConnection = MusicManager.destroyConnection(guildId);

        final Mono<Void> deleteGuild = DatabaseManager.getGuilds()
                .getDBGuild(guildId)
                .flatMap(DBGuild::delete);

        return destroyVoiceConnection
                .and(deleteGuild);
    }

}
