package me.shadorc.discordbot.events.music;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.command.Emoji;

public class AudioEventListener extends AudioEventAdapter {

	private final GuildMusicManager musicManager;

	private int errorCount;

	public AudioEventListener(GuildMusicManager musicManager) {
		super();
		this.musicManager = musicManager;
		this.errorCount = 0;
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Currently playing: **" + StringUtils.formatTrackName(track.getInfo()) + "**", musicManager.getChannel());
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) {
			errorCount = 0; // Everything seems to be fine, reset error count.

			if(musicManager.getScheduler().isRepeating()) {
				musicManager.getScheduler().queue(track.makeClone());
			} else if(!musicManager.getScheduler().nextTrack()) {
				musicManager.end();
			}
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		errorCount++;

		String errMessage = Jsoup.parse(err.getMessage().replace("Watch on YouTube", "")).text().trim();

		if(errorCount <= 3) {
			BotUtils.sendMessage(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase() + ". I'll try to play the next available song.", musicManager.getChannel());
		}

		if(errorCount == 3) {
			BotUtils.sendMessage(Emoji.RED_FLAG + " Too many errors in a row, I will ignore them until finding a music that can be played.", musicManager.getChannel());
			LogUtils.info("{Guild ID: " + musicManager.getChannel().getGuild().getLongID() + "} Too many errors in a row. "
					+ "Shadbot will ignore them until finding a music that can be played.");
		}

		LogUtils.info("{Guild ID: " + musicManager.getChannel().getGuild().getLongID() + "} " + (errorCount > 3 ? "(Ignored) " : "") + "Track exception: " + errMessage);

		if(!musicManager.getScheduler().nextTrack()) {
			musicManager.end();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		BotUtils.sendMessage(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll try to play the next available song.", musicManager.getChannel());
		LogUtils.warn("{Guild ID: " + musicManager.getChannel().getGuild().getLongID() + "} Music stuck, skipping it.");

		if(!musicManager.getScheduler().nextTrack()) {
			musicManager.end();
		}
	}
}
