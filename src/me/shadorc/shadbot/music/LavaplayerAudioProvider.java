package me.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;

import discord4j.voice.AudioProvider;

public class LavaplayerAudioProvider extends AudioProvider {

	private final AudioPlayer audioPlayer;
	private final MutableAudioFrame frame;

	public LavaplayerAudioProvider(AudioPlayer audioPlayer) {
		super();
		this.audioPlayer = audioPlayer;
		this.frame = new MutableAudioFrame();
		this.frame.setBuffer(this.getBuffer());
	}

	@Override
	public boolean provide() {
		boolean didProvide = audioPlayer.provide(frame);
		if(didProvide) {
			this.getBuffer().flip();
		}
		return didProvide;
	}

}
