package com.locibot.locibot.music;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.voice.AudioProvider;

import java.nio.ByteBuffer;

public class LavaplayerAudioProvider extends AudioProvider {

    private final AudioPlayer audioPlayer;
    private final MutableAudioFrame frame;

    public LavaplayerAudioProvider(AudioPlayer audioPlayer) {
        super(ByteBuffer.allocate(StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()));
        this.audioPlayer = audioPlayer;
        this.frame = new MutableAudioFrame();
        this.frame.setBuffer(this.getBuffer());
    }

    @Override
    public boolean provide() {
        final boolean didProvide = this.audioPlayer.provide(this.frame);
        if (didProvide) {
            this.getBuffer().flip();
        }
        return didProvide;
    }

}
