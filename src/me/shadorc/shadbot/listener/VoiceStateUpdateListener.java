package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class VoiceStateUpdateListener {

	public static void onVoiceStateUpdateEvent(VoiceStateUpdateEvent event) {
		final Snowflake selfId = event.getClient().getSelfId().get();
		final Snowflake userId = event.getCurrent().getUserId();

		if(userId.equals(selfId)) {
			onBotEvent(event);
		} else {
			onUserEvent(event);
		}
	}

	private static void onBotEvent(VoiceStateUpdateEvent event) {
		// If the bot is no more in a voice channel, it left
		if(!event.getCurrent().getChannelId().isPresent()) {
			final Snowflake guildId = event.getCurrent().getGuildId();

			final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
			if(guildMusic != null) {
				guildMusic.destroy();
				LogUtils.infof("{Guild ID: %d} Voice channel left.", guildId.asLong());
			}
		}
	}

	// TODO
	private static synchronized void onUserEvent(VoiceStateUpdateEvent event) {
		// If the bot is in a voice channel...
		// ...and the guild music for this guild is not null...
		// if(isAlone && !guildMusic.isLeavingScheduled()) {
		// guildMusic.getScheduler().getAudioPlayer().setPaused(true);
		// guildMusic.scheduleLeave();
		// return Emoji.INFO + " Nobody is listening anymore, music paused. I will leave the voice channel in 1 minute.";
		// } else if(!isAlone && guildMusic.isLeavingScheduled()) {
		// guildMusic.getScheduler().getAudioPlayer().setPaused(false);
		// guildMusic.cancelLeave();
		// return Emoji.INFO + " Somebody joined me, music resumed.";
		// }
	}

}
