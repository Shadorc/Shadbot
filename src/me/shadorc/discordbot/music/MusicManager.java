package me.shadorc.discordbot.music;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.sound.sampled.UnsupportedAudioFileException;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.AudioPlayer.Track;

public class MusicManager {

	private int volume;
	private AudioPlayer audioPlayer;

	public MusicManager(IGuild guild) {
		this.audioPlayer = AudioPlayer.getAudioPlayerForGuild(guild);
	}

	public void setVolume(int volume) {
		this.volume = Math.max(0, Math.min(100, volume));
		this.audioPlayer.setVolume(volume/100f);
	}

	public int getVolume() {
		return this.volume;
	}

	public void queue(File file) throws IOException, UnsupportedAudioFileException {
		this.audioPlayer.queue(file);
	}

	public void stop() {
		this.audioPlayer.clear();
	}

	public void setPaused(boolean isPaused) {
		this.audioPlayer.setPaused(isPaused);
	}

	public void next() {
		this.audioPlayer.skip();
	}

	public String getCurrentTrackName() {
		return ((File) this.audioPlayer.getCurrentTrack().getMetadata().get("file")).getName();
	}

	public List <Track> getPlaylist() {
		return this.audioPlayer.getPlaylist();
	}

	public boolean isPaused() {
		return this.audioPlayer.isPaused();
	}

	public boolean isPlaying() {
		return this.audioPlayer.getCurrentTrack() != null;
	}
}
