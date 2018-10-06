// TODO: Implement
// package me.shadorc.shadbot.music;
//
// import java.util.Objects;
//
// import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
// import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
//
// import discord4j.voice.AudioProvider;
//
// public class MusicProvider implements AudioProvider {
//
// private final AudioPlayer audioPlayer;
// private final GuildMusic guildMusic;
// private AudioFrame lastFrame;
//
// public MusicProvider(AudioPlayer audioPlayer, GuildMusic guildMusic) {
// this.audioPlayer = audioPlayer;
// this.guildMusic = guildMusic;
// }
//
// @Override
// public boolean isDone() {
// return !this.guildMusic.isInVoiceChannel();
// }
//
// @Override
// public byte[] provide() {
// if(this.lastFrame == null) {
// this.lastFrame = this.audioPlayer.provide();
// }
//
// final byte[] data = this.lastFrame == null ? null : this.lastFrame.getData();
// this.lastFrame = null;
//
// return Objects.requireNonNullElse(data, new byte[0]);
// }
//
// }
