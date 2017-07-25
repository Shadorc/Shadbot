package me.shadorc.discordbot.music;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.utility.Utils;

public class TrackScheduler extends AudioEventAdapter {

	private static final int DEFAULT_VOLUME = 20;

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

	public boolean nextTrack() {
		return audioPlayer.startTrack(queue.poll(), false);
	}

	public String getCurrentTrackName() {
		return Utils.formatTrackName(this.audioPlayer.getPlayingTrack().getInfo());
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

	public void setPaused(boolean isPaused) {
		this.audioPlayer.setPaused(isPaused);
	}

	public boolean isPaused() {
		return this.audioPlayer.isPaused();
	}

	public boolean isPlaying() {
		return (audioPlayer.getPlayingTrack() != null);
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if(endReason.mayStartNext) {
			this.nextTrack();
		}
	}
}