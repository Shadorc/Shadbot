package me.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.log.LogUtils;

public class AudioEventListener extends AudioEventAdapter {

	private final GuildMusic guildMusic;
	private int errorCount;

	public AudioEventListener(GuildMusic guildMusic) {
		super();
		this.guildMusic = guildMusic;
		this.errorCount = 0;
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		BotUtils.sendMessage(
				String.format(Emoji.MUSICAL_NOTE + " Currently playing: **%s**",
						FormatUtils.formatTrackName(track.getInfo())),
				guildMusic.getMessageChannel())
				.subscribe();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) {
			errorCount = 0; // Everything seems to be fine, reset error counter.
			this.nextOrEnd();
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		errorCount++;

		final String errMessage = TextUtils.cleanLavaplayerErr(err);

		if(errorCount <= 3) {
			BotUtils.sendMessage(
					String.format(Emoji.RED_CROSS + " Sorry, %s. I'll try to play the next available song.", errMessage.toLowerCase()),
					guildMusic.getMessageChannel())
					.subscribe();
		}

		if(errorCount == 3) {
			BotUtils.sendMessage(
					Emoji.RED_FLAG + " Too many errors in a row, I will ignore them until I find a music that can be played.",
					guildMusic.getMessageChannel())
					.subscribe();
			LogUtils.infof("{Guild ID: %d} Too many errors in a row. They will be ignored until a music can be played.",
					guildMusic.getGuildId().asLong());
		}

		LogUtils.infof("{Guild ID: %d} %sTrack exception: %s",
				guildMusic.getGuildId().asLong(), errorCount > 3 ? "(Ignored) " : "", errMessage);

		this.nextOrEnd();
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		BotUtils.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll try to play the next available song.",
				guildMusic.getMessageChannel())
				.subscribe();
		LogUtils.warn(guildMusic.getClient(), String.format("{Guild ID: %d} Music stuck, skipping it.", guildMusic.getGuildId().asLong()));

		this.nextOrEnd();
	}

	private void nextOrEnd() {
		if(!guildMusic.getScheduler().nextTrack()) {
			guildMusic.end().subscribe();
		}
	}
}
