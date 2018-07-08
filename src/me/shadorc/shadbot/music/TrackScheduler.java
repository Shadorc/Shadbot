package me.shadorc.shadbot.music;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.shadbot.data.stats.VariousStatsManager;
import me.shadorc.shadbot.data.stats.VariousStatsManager.VariousEnum;
import me.shadorc.shadbot.utils.NumberUtils;

public class TrackScheduler {

	public enum RepeatMode {
		NONE, SONG, PLAYLIST;
	}

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
		audioPlayer.setVolume(NumberUtils.between(volume, 0, 100));
	}

	public void setRepeatMode(RepeatMode repeatMode) {
		this.repeatMode = repeatMode;
	}

	/**
	 * @return {@code true} if the track was started, {@code false} if it was added to the queue
	 */
	public boolean startOrQueue(AudioTrack track, boolean first) {
		VariousStatsManager.log(VariousEnum.MUSICS_LOADED);

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

	/**
	 * @return {@code true} if the track was started, {@code false} otherwise
	 */
	public boolean nextTrack() {
		switch (repeatMode) {
			case PLAYLIST:
				queue.offer(currentTrack.makeClone());
			case NONE:
				this.currentTrack = queue.poll();
				return audioPlayer.startTrack(currentTrack, false);
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
		final AudioTrack track = audioPlayer.getPlayingTrack();
		final long newPosition = track.getPosition() + time;
		track.setPosition(NumberUtils.between(newPosition, 0, track.getDuration()));
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

	public void destroy() {
		audioPlayer.destroy();
		this.clearPlaylist();
	}
}