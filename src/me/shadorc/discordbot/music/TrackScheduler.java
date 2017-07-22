package me.shadorc.discordbot.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class TrackScheduler extends AudioEventAdapter {

	private static final int DEFAULT_VOLUME = 15;

	private final AudioPlayer audioPlayer;
	private final BlockingQueue<AudioTrack> queue;

	public TrackScheduler(AudioPlayer player) {
		this.audioPlayer = player;
		this.queue = new LinkedBlockingQueue<>();
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

	public void nextTrack() {
		audioPlayer.startTrack(queue.poll(), false);
	}

	public boolean isPlaying() {
		return (audioPlayer.getPlayingTrack() != null);
	}

	public void setPaused(boolean isPaused) {
		this.audioPlayer.setPaused(isPaused);
	}

	public String getCurrentTrackName() {
		return this.audioPlayer.getPlayingTrack().getInfo().title;
	}

	public BlockingQueue <AudioTrack> getPlaylist() {
		return queue;
	}

	public boolean isPaused() {
		return this.audioPlayer.isPaused();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) {
			this.nextTrack();
		}
	}

	public void setVolume(int volume) {
		audioPlayer.setVolume(Math.max(0, Math.min(150, volume)));
	}

	public int getVolume() {
		return audioPlayer.getVolume();
	}
}
