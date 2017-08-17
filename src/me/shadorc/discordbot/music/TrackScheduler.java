package me.shadorc.discordbot.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IGuild;

public class TrackScheduler {

	private final AudioPlayer audioPlayer;
	private final BlockingQueue<AudioTrack> queue;

	private boolean isRepeating;

	public TrackScheduler(IGuild guild, AudioPlayer player) {
		this.audioPlayer = player;
		this.queue = new LinkedBlockingQueue<>();
		this.isRepeating = false;
		this.setVolume(Integer.parseInt(Storage.getSetting(guild, Setting.DEFAULT_VOLUME).toString()));
	}

	public void queue(AudioTrack track) {
		if(!audioPlayer.startTrack(track, true)) {
			queue.offer(track);
		}
	}

	public boolean nextTrack() {
		return audioPlayer.startTrack(queue.poll(), false);
	}

	public void stop() {
		audioPlayer.stopTrack();
	}

	public String getCurrentTrackName() {
		return StringUtils.formatTrackName(audioPlayer.getPlayingTrack().getInfo());
	}

	public BlockingQueue<AudioTrack> getPlaylist() {
		return queue;
	}

	public int getVolume() {
		return audioPlayer.getVolume();
	}

	public void setVolume(int volume) {
		audioPlayer.setVolume(Math.max(0, Math.min(100, volume)));
	}

	public void setRepeatEnabled(boolean enabled) {
		this.isRepeating = enabled;
	}

	public void setPaused(boolean isPaused) {
		audioPlayer.setPaused(isPaused);
	}

	public boolean isRepeating() {
		return isRepeating;
	}

	public boolean isPaused() {
		return audioPlayer.isPaused();
	}

	public boolean isPlaying() {
		return audioPlayer.getPlayingTrack() != null;
	}
}