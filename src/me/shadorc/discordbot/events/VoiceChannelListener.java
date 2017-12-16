package me.shadorc.discordbot.events;

import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.shards.ShardManager;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceChannelEvent;
import sx.blah.discord.handle.impl.events.guild.voice.VoiceDisconnectedEvent;

@SuppressWarnings("ucd")
public class VoiceChannelListener {

	@EventSubscriber
	public void onVoiceChannelEvent(VoiceChannelEvent event) {
		ShardManager.getThreadPool(event.getGuild()).execute(() -> {
			long startTime = System.currentTimeMillis();
			if(event instanceof VoiceDisconnectedEvent) {
				this.onVoiceDisconnectedEvent((VoiceDisconnectedEvent) event);
			}
			float elapsedSec = (System.currentTimeMillis() - startTime) / 1000f;
			if(elapsedSec > 10) {
				LogUtils.info("{DEBUG} " + event.getClass().getSimpleName() + " | Long event detected ! "
						+ "Duration: " + String.format("%.1f", elapsedSec) + "s.");
			}
		});
	}

	private void onVoiceDisconnectedEvent(VoiceDisconnectedEvent event) {
		GuildMusicManager musicManager = GuildMusicManager.getGuildMusicManager(event.getGuild());
		if(musicManager != null) {
			musicManager.delete();
			LogUtils.info("{Guild ID: " + event.getGuild().getLongID() + ")} Voice channel left.");

			// If Shadbot is disconnected with status code 1008 and reason "NullPointerException", it will still be in the voice channel
			// If not, this line will do nothing
			if(event.getVoiceChannel() != null && event.getVoiceChannel().getShard().isReady()) {
				event.getVoiceChannel().leave();
			}
		}
	}
}
