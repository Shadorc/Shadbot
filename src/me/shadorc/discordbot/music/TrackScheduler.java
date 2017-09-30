package me.shadorc.discordbot.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.utils.StringUtils;

public class TrackScheduler {

	private final AudioPlayer audioPlayer;
	private final BlockingQueue<AudioTrack> queue;

	private boolean isRepeating;

	public TrackScheduler(AudioPlayer audioPlayer, int defaultVolume) {
		this.audioPlayer = audioPlayer;
		this.queue = new LinkedBlockingQueue<>();
		this.isRepeating = false;
		this.setVolume(defaultVolume);
	}

	public void queue(AudioTrack track) {
		if(!audioPlayer.startTrack(track, true)) {
			queue.offer(track);
		}
	}

	public boolean nextTrack() {
		return audioPlayer.startTrack(queue.poll(), false);
	}

	public void skipTo(int num) {
		AudioTrack track = null;
		for(int i = 0; i < num; i++) {
			track = queue.poll();
		}
		audioPlayer.playTrack(track.makeClone());
	}

	public long changePosition(long time) {
		long newPosition = audioPlayer.getPlayingTrack().getPosition() + time;
		if(newPosition < 0 || newPosition > audioPlayer.getPlayingTrack().getDuration()) {
			throw new IllegalArgumentException();
		}
		audioPlayer.getPlayingTrack().setPosition(newPosition);
		return newPosition;
	}

	public void shufflePlaylist() {
		List<AudioTrack> tempList = new ArrayList<>(queue);
		Collections.shuffle(tempList);
		queue.clear();
		queue.addAll(tempList);
	}

	public void clearPlaylist() {
		queue.clear();
	}

	public String getPlayingTrackName() {
		return StringUtils.formatTrackName(audioPlayer.getPlayingTrack().getInfo());
	}

	public BlockingQueue<AudioTrack> getPlaylist() {
		return queue;
	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}

	public void setVolume(int volume) {
		audioPlayer.setVolume(Math.max(0, Math.min(100, volume)));
	}

	public void setRepeat(boolean enabled) {
		this.isRepeating = enabled;
	}

	public boolean isRepeating() {
		return isRepeating;
	}

	public boolean isPlaying() {
		return audioPlayer.getPlayingTrack() != null;
	}

	public boolean isStopped() {
		return queue.isEmpty() && !this.isPlaying();
	}
}