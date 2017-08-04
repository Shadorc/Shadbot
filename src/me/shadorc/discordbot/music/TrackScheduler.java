package me.shadorc.discordbot.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.utils.StringUtils;

public class TrackScheduler {

	private static final int DEFAULT_VOLUME = 15;

	private final AudioPlayer audioPlayer;
	private final BlockingQueue<AudioTrack> queue;

	private boolean isRepeating;

	public TrackScheduler(AudioPlayer player) {
		this.audioPlayer = player;
		this.queue = new LinkedBlockingQueue<>();
		this.isRepeating = false;
		this.setVolume(DEFAULT_VOLUME);
	}

	public void queue(AudioTrack track) {
		if(!audioPlayer.startTrack(track, true)) {
			queue.offer(track);
		}
	}

	public void stop() {
		audioPlayer.stopTrack();
	}

	public boolean nextTrack() {
		return audioPlayer.startTrack(queue.poll(), false);
	}

	public String getCurrentTrackName() {
		return StringUtils.formatTrackName(audioPlayer.getPlayingTrack().getInfo());
	}

	public BlockingQueue <AudioTrack> getPlaylist() {
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
		return (audioPlayer.getPlayingTrack() != null);
	}
}