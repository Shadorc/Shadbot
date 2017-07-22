package me.shadorc.discordbot.music;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.util.audio.AudioPlayer;

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

	public void start(File file) throws IOException, UnsupportedAudioFileException {
		this.audioPlayer.queue(file);
	}

	public void stop() {
		this.audioPlayer.clear();
	}

}
