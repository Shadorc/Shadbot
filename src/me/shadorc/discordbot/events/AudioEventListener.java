package me.shadorc.discordbot.events;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class AudioEventListener extends AudioEventAdapter {

	private final IGuild guild;
	private final TrackScheduler scheduler;

	private IChannel channel;
	private int errorCount;

	public AudioEventListener(IGuild guild, TrackScheduler scheduler) {
		super();
		this.guild = guild;
		this.scheduler = scheduler;
		this.errorCount = 0;
	}

	public void setChannel(IChannel channel) {
		this.channel = channel;
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		BotUtils.send(Emoji.MUSICAL_NOTE + " Currently playing: **" + StringUtils.formatTrackName(track.getInfo()) + "**", channel);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) {
			errorCount = 0; // Everything seems to be fine, reset error count.

			if(scheduler.isRepeating()) {
				scheduler.queue(track.makeClone());
			} else if(!scheduler.nextTrack()) {
				GuildMusicManager.getGuildMusicManager(guild).end();
			}
		}
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		errorCount++;

		String errMessage = Jsoup.parse(err.getMessage().replace("Watch on YouTube", "")).text().trim();

		if(errorCount <= 3) {
			BotUtils.send(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase() + ". I'll try to play the next available song.", channel);
		}

		if(errorCount == 3) {
			BotUtils.send(Emoji.RED_FLAG + " Too many errors in a row, I will ignore them until finding a music that can be played.", channel);
			LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} Too many errors in a row. "
					+ "Shadbot will ignore them until finding a music that can be played.");
		}

		LogUtils.info("{Guild ID: " + channel.getGuild().getLongID() + "} " + (errorCount > 3 ? "(Ignored) " : "") + "Track exception: " + errMessage);

		if(!scheduler.nextTrack()) {
			GuildMusicManager.getGuildMusicManager(guild).end();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		BotUtils.send(Emoji.RED_EXCLAMATION + " Music seems stuck, I'll try to play the next available song.", channel);
		LogUtils.warn("{Guild ID: " + channel.getGuild().getLongID() + "} Music stuck, skipping it.");

		if(!scheduler.nextTrack()) {
			GuildMusicManager.getGuildMusicManager(guild).end();
		}
	}
}
