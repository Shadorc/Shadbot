package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.music.MusicManager;
import me.shadorc.shadbot.utils.LogUtils;
import reactor.core.publisher.Mono;

public class GuildListener {

    public static Mono<Void> onGuildCreate(GuildCreateEvent event) {
        return Mono.fromRunnable(() -> {
            if (Shadbot.getShards().get(event.getClient().getConfig().getShardIndex()).isFullyReady()) {
                final long guildId = event.getGuild().getId().asLong();
                final int memberCount = event.getGuild().getMemberCount().orElse(-1);
                LogUtils.info("{Guild ID: %d} Connected (%d users).", guildId, memberCount);
            }
        });
    }

    public static Mono<Void> onGuildDelete(GuildDeleteEvent event) {
        return Mono.fromRunnable(() -> {
            LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong());
            MusicManager.getInstance().removeConnection(event.getGuildId());
            DatabaseManager.getInstance().removeDBGuild(event.getGuildId());
        });
    }
}
