package me.shadorc.shadbot.music;

import java.util.concurrent.ConcurrentHashMap;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager {

	public static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
	public static final ConcurrentHashMap<Long, GuildMusic> GUILD_MUSIC_MAP = new ConcurrentHashMap<>();

	static {
		AudioSourceManagers.registerRemoteSources(GuildMusicManager.PLAYER_MANAGER);
	}

	public static GuildMusic createGuildMusic(IGuild guild) {
		GuildMusic guildMusic = new GuildMusic(guild, PLAYER_MANAGER);
		GUILD_MUSIC_MAP.put(guild.getLongID(), guildMusic);

		guild.getAudioManager().setAudioProvider(guildMusic.getAudioProvider());

		return guildMusic;
	}

	public static void stop() {
		GUILD_MUSIC_MAP.values().stream().forEach(GuildMusic::leaveVoiceChannel);
	}

}
