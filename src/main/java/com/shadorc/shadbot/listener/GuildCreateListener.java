package com.shadorc.shadbot.listener;

import com.shadorc.shadbot.cache.CacheManager;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import reactor.core.publisher.Mono;

import static com.shadorc.shadbot.Shadbot.DEFAULT_LOGGER;

public class GuildCreateListener implements EventListener<GuildCreateEvent> {

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

            CacheManager.getInstance().getGuildOwnersCache().save(event.getGuild().getId(), event.getGuild().getOwnerId());
        });
    }

}
