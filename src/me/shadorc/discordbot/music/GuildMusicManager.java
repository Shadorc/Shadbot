package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager {

	private static AudioPlayerManager playerManager;
	private static Map<Long, GuildMusicManager> musicManagers;

	public static void init() {
		musicManagers = new HashMap<>();
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
		AudioSourceManagers.registerLocalSource(playerManager);
	}

	public static synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
		GuildMusicManager musicManager = musicManagers.get(guild.getLongID());

		if(musicManager == null) {
			musicManager = new GuildMusicManager(playerManager);
			musicManagers.put(guild.getLongID(), musicManager);
		}

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}

	public static AudioPlayerManager getAudioPlayerManager() {
		return playerManager;
	}

	private AudioPlayer player;
	private TrackScheduler scheduler;

	public GuildMusicManager(AudioPlayerManager manager) {
		this.player = manager.createPlayer();
		this.scheduler = new TrackScheduler(player);
		this.player.addListener(scheduler);
	}

	public AudioProvider getAudioProvider() {
		return new AudioProvider(player);
	}

	public AudioPlayer getAudioPlayer() {
		return player;
	}

	public TrackScheduler getScheduler() {
		return scheduler;
	}
}
