package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class VoiceChannelListener {

	public static void onVoiceDisconnectedEvent(VoiceStateUpdateEvent event) {
		event.getClient().getSelf().subscribe(self -> {
			// This is not an event from the bot
			if(!event.getCurrent().getUserId().equals(self.getId())) {
				return;
			}

			// This is not a disconnected event, the bot is still in a voice channel
			if(event.getCurrent().getChannelId().isPresent()) {
				return;
			}

			final Snowflake guildId = event.getCurrent().getGuildId();

			GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
			if(guildMusic != null) {
				guildMusic.delete();
				LogUtils.infof("{Guild ID: %s} Voice channel left.", guildId);

				/*
				 * TODO: Does it still need to be managed ? How to detect if the bot is still in the voice channel ? If the bot is disconnected with status code 1008
				 * and reason "NullPointerException", it will still be in the voice channel If not, this line will do nothing
				 */
				if(event.getCurrent().getChannelId().isPresent()) {
					// TODO: leave voice channel
				}
			}
		});
	}
}
