package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.cache.GuildOwnersCache;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import io.prometheus.client.Gauge;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class GuildCreateListener implements EventListener<GuildCreateEvent> {

    public static final Gauge GUILD_COUNT_GAUGE = Gauge.build()
            .namespace("shadbot")
            .name("guild_count")
            .help("Guild count")
            .register();

    public static final GuildOwnersCache GUILD_OWNERS_CACHE = new GuildOwnersCache();

    @Override
    public Class<GuildCreateEvent> getEventType() {
        return GuildCreateEvent.class;
    }

    @Override
    public Mono<Void> execute(GuildCreateEvent event) {
        return Mono.fromRunnable(() -> {
            final long guildId = event.getGuild().getId().asLong();
            final int memberCount = event.getGuild().getMemberCount();
            DEFAULT_LOGGER.debug("{Guild ID: {}} Connected ({} users)", guildId, memberCount);

            GUILD_COUNT_GAUGE.inc();
            GUILD_OWNERS_CACHE.put(event.getGuild().getId(), event.getGuild().getOwnerId());
        });
    }

}
