package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.guild.GuildDeleteEvent;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.music.GuildVoiceManager;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class GuildListener {

	public static Mono<Void> onGuildCreate(GuildCreateEvent event) {
		return Mono.fromRunnable(() -> {
			if(Shadbot.getShards().get(event.getClient().getConfig().getShardIndex()).isFullyReady()) {
				final long guildId = event.getGuild().getId().asLong();
				final int memberCount = event.getGuild().getMemberCount().orElse(-1);
				LogUtils.info("{Guild ID: %d} Connected (%d users).", guildId, memberCount);
			}
		});
	}

	public static Mono<Void> onGuildDelete(GuildDeleteEvent event) {
		return Mono.fromRunnable(() -> {
			LogUtils.info("{Guild ID: %d} Disconnected.", event.getGuildId().asLong());
			final GuildMusic guildMusic = GuildMusicManager.get(event.getGuildId());
			if(guildMusic != null) {
				guildMusic.destroy();
			}
			GuildVoiceManager.remove(event.getGuildId());
			Shadbot.getDatabase().removeDBGuild(event.getGuildId());
		});
	}
}
