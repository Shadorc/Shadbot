package me.shadorc.shadbot.listener;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class VoiceStateUpdateListener {

	// TODO: Rework
	public static void onVoiceStateUpdateEvent(VoiceStateUpdateEvent event) {
		// TODO: Use DiscordClient#getSelfId
		event.getClient().getSelf().subscribe(self -> {
			// This is not an event from the bot
			if(event.getCurrent().getUserId().equals(self.getId())) {
				// TODO: && !event.getCurrent().isPresent()) {
				final Snowflake guildId = event.getCurrent().getGuildId();

				GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guildId);
				if(guildMusic != null) {
					guildMusic.destroy();
					LogUtils.infof("{Guild ID: %s} Voice channel left.", guildId);
				}
				return;
			}

			check(null);
		});
	}

	private static synchronized void check(Guild guild) {
		// TODO avoid blocking, use DiscordClient#getSelfId
		guild.getClient()
				.getMemberById(guild.getId(), guild.getClient().getSelf().block().getId())
				.flatMap(member -> member.getVoiceState())
				.subscribe(voiceState -> {

					// The bot is not in a voice channel
					if(!voiceState.getChannelId().isPresent()) {
						return;
					}

					GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(guild.getId());
					if(guildMusic == null) {
						return;
					}

					if(isAlone(guild) && !guildMusic.isLeavingScheduled()) {
						BotUtils.sendMessage(Emoji.INFO + " Nobody is listening anymore, music paused. "
								+ "I will leave the voice channel in 1 minute.",
								guildMusic.getMessageChannel());
						guildMusic.getScheduler().getAudioPlayer().setPaused(true);
						guildMusic.scheduleLeave();

					} else if(!isAlone(guild) && guildMusic.isLeavingScheduled()) {
						BotUtils.sendMessage(Emoji.INFO + " Somebody joined me, music resumed.",
								guildMusic.getMessageChannel());
						guildMusic.getScheduler().getAudioPlayer().setPaused(false);
						guildMusic.cancelLeave();
					}
				});
	}

	// TODO
	private static boolean isAlone(Object object) {
		return true;
		// return voiceChannel.getConnectedUsers().stream().filter(user -> !user.isBot()).count() == 0;
	}

}
