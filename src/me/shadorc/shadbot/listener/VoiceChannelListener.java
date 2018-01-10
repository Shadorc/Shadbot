package me.shadorc.shadbot.listener;

import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.shard.ShardManager;
import me.shadorc.shadbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceChannelEvent;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceDisconnectedEvent;

@SuppressWarnings("ucd")
public class VoiceChannelListener {

	@EventSubscriber
	public void onVoiceChannelEvent(VoiceChannelEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			if(event instanceof VoiceDisconnectedEvent) {
				this.onVoiceDisconnectedEvent((VoiceDisconnectedEvent) event);
			}
		});
	}

	private void onVoiceDisconnectedEvent(VoiceDisconnectedEvent event) {
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
