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

	public AudioEventListener(IGuild guild, TrackScheduler scheduler) {
		super();
		this.guild = guild;
		this.scheduler = scheduler;
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
		// Create a new Thread avoid java.net.SocketException by leaving the time to the sockets to close
		new Thread(() -> {
			if(endReason.mayStartNext) {
				if(scheduler.isRepeating()) {
					scheduler.queue(track.makeClone());
				} else if(!scheduler.nextTrack()) {
					BotUtils.sendMessage(Emoji.EXCLAMATION + " End of the playlist.", channel);
					GuildMusicManager.getGuildAudioPlayer(guild).leaveVoiceChannel();
				}
			}
		}).start();
	}

	@Override
	public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException err) {
		LogUtils.error(err.getMessage() + ". Sorry for the inconveniences, I'll try to play the next available song.", channel);
		if(!scheduler.nextTrack()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " End of the playlist.", channel);
			GuildMusicManager.getGuildAudioPlayer(guild).leaveVoiceChannel();
		}
	}

	@Override
	public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
		BotUtils.sendMessage("Music seems stuck, I'll try to play the next available song.", channel);
		LogUtils.warn("Music was stuck, skipping it. (Threshold: " + thresholdMs + " ms)");
		if(!scheduler.nextTrack()) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " End of the playlist.", channel);
			GuildMusicManager.getGuildAudioPlayer(guild).leaveVoiceChannel();
		}
	}
}
