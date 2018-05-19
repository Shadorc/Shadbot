package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class VoiceChannelListener {

	public static void onVoiceDisconnectedEvent(VoiceDisconnectedEvent event) {
		GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(event.getGuild().getLongID());
		if(guildMusic != null) {
			guildMusic.delete();
			LogUtils.infof("{Guild ID: %d} Voice channel left.", event.getGuild().getLongID());

			// If Shadbot is disconnected with status code 1008 and reason "NullPointerException", it will still be in the voice channel
			// If not, this line will do nothing
			if(event.getVoiceChannel() != null && event.getVoiceChannel().getShard().isReady()) {
				event.getVoiceChannel().leave();
			}
		}
	}
}
