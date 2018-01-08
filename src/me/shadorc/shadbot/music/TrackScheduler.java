package me.shadorc.shadbot.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.shadbot.data.stats.Stats.VariousEnum;
import me.shadorc.shadbot.data.stats.StatsManager;

public class TrackScheduler {

	private final AudioPlayer audioPlayer;
	private final BlockingDeque<AudioTrack> queue;

	private RepeatMode repeatMode;
	private AudioTrack currentTrack;

	public TrackScheduler(AudioPlayer audioPlayer, int defaultVolume) {
		this.audioPlayer = audioPlayer;
		this.queue = new LinkedBlockingDeque<>();
		this.repeatMode = RepeatMode.NONE;
		this.setVolume(defaultVolume);
	}

	/**
	 * @return true if the music has been started, false if it was added to the queue
	 */
	public boolean startOrQueue(AudioTrack track, boolean first) {
		StatsManager.increment(VariousEnum.MUSICS_LOADED);

		// The track has been started
		if(audioPlayer.startTrack(track.makeClone(), true)) {
			this.currentTrack = track;
			return true;
		} else if(first) {
			queue.offerFirst(track);
		} else {
			queue.offerLast(track);
		}
		return false;
	}

	public boolean nextTrack() {
		switch (repeatMode) {
			case PLAYLIST:
				queue.offer(currentTrack.makeClone());
			case NONE:
				this.currentTrack = queue.poll();
				return audioPlayer.startTrack(currentTrack.makeClone(), false);
			case SONG:
				audioPlayer.playTrack(currentTrack.makeClone());
				break;
		}
		return true;
	}

	public void skipTo(int num) {
		AudioTrack track = null;
		for(int i = 0; i < num; i++) {
			track = queue.poll();
		}
		audioPlayer.playTrack(track.makeClone());
		this.currentTrack = track;
	}

	public long changePosition(long time) {
		long newPosition = audioPlayer.getPlayingTrack().getPosition() + time;
		newPosition = Math.max(0, Math.min(audioPlayer.getPlayingTrack().getDuration(), newPosition));
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

	public BlockingQueue<AudioTrack> getPlaylist() {
		return queue;
	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}

	public RepeatMode getRepeatMode() {
		return repeatMode;
	}

	public boolean isPlaying() {
		return audioPlayer.getPlayingTrack() != null;
	}

	public boolean isStopped() {
		return queue.isEmpty() && !this.isPlaying();
	}

	public void setVolume(int volume) {
		audioPlayer.setVolume(Math.max(0, Math.min(100, volume)));
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		this.repeatMode = repeatMode;
	}
}