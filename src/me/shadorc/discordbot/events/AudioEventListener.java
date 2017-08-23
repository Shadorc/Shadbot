package me.shadorc.discordbot.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.music.TrackScheduler;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
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
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Currently playing: **" + StringUtils.formatTrackName(track.getInfo()) + "**", channel);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		// Create a new Thread avoid SocketException by leaving the time to the sockets to close
		new Thread(() -> {
			if(endReason.mayStartNext) {
				errorCount = 0; // Everything seems to be fine, reset error count.
				if(scheduler.isRepeating()) {
					scheduler.queue(track.makeClone());
				} else if(!scheduler.nextTrack()) {
					GuildMusicManager.getGuildMusicManager(guild).end();
				}
			}
		}).start();
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		errorCount++;
		if(errorCount > 3) {
			LogUtils.error("Something went terribly wrong, too many errors in a row. I'm stopping music to avoid spam. "
					+ "You can retry after this, sorry for the inconvenience.", err, channel);
			GuildMusicManager.getGuildMusicManager(guild).leaveVoiceChannel();
			return;
		}

		BotUtils.sendMessage(Emoji.GEAR + " " + err.getMessage() + ". Sorry for the inconveniences, I'll try to play the next available song.", channel);
		LogUtils.warn("{AudioEventListener} {Guild: " + channel.getGuild().getName()
				+ " (ID: " + channel.getGuild().getStringID() + ")} Track exception: " + err.getMessage());

		if(!scheduler.nextTrack()) {
			GuildMusicManager.getGuildMusicManager(guild).end();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		BotUtils.sendMessage(Emoji.GEAR + " Music seems stuck, I'll try to play the next available song.", channel);
		LogUtils.warn("{AudioEventListener} {Guild: " + channel.getGuild().getName()
				+ " (ID: " + channel.getGuild().getStringID() + ")} Music stuck, skipping it.");

		if(!scheduler.nextTrack()) {
			GuildMusicManager.getGuildMusicManager(guild).end();
		}
	}
}
