package me.shadorc.discordbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class AudioEventListener extends AudioEventAdapter {

	private final IGuild guild;
	private final TrackScheduler scheduler;
	private IChannel channel;

	public AudioEventListener(IGuild guild, TrackScheduler scheduler) {
		this.guild = guild;
		this.scheduler = scheduler;
	}

	public void setChannel(IChannel channel) {
		this.channel = channel;
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		//Create a new Thread avoid java.net.SocketException by leaving the time to the sockets to close
		new Thread(() -> {
			if(endReason.mayStartNext) {
				if(scheduler.isRepeating()) {
					scheduler.queue(track.makeClone());
				} else if(!scheduler.nextTrack()) {
					BotUtils.sendMessage(Emoji.WARNING + " Fin de la playlist.", channel);
					GuildMusicManager.getGuildAudioPlayer(guild).leave();
				}
			}
		}).start();
	}

	@Override
	public void onTrackStart(AudioPlayer player, AudioTrack track) {
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " Lecture en cours : *" + StringUtils.formatTrackName(track.getInfo()) + "*", channel);
	}
}
