package me.shadorc.shadbot.music;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Snowflake;

public class GuildMusicManager {

	private static final AudioPlayerManager AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
	private static final Map<Snowflake, GuildMusic> GUILD_MUSIC_MAP = new ConcurrentHashMap<>();

	static {
		AUDIO_PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		AudioSourceManagers.registerRemoteSources(AUDIO_PLAYER_MANAGER);
	}

	public static AudioPlayerManager getAudioPlayerManager() {
		return AUDIO_PLAYER_MANAGER;
	}

	public static GuildMusic createGuildMusic(DiscordClient client, Snowflake guildId) {
		final GuildMusic guildMusic = new GuildMusic(client, guildId, AUDIO_PLAYER_MANAGER.createPlayer());
		GUILD_MUSIC_MAP.put(guildId, guildMusic);
		return guildMusic;
	}

	public static Collection<GuildMusic> getGuildMusics() {
		return GUILD_MUSIC_MAP.values();
	}

	public static GuildMusic getGuildMusic(Snowflake guildId) {
		return GUILD_MUSIC_MAP.get(guildId);
	}

	public static void removeGuildMusic(Snowflake guildId) {
		GUILD_MUSIC_MAP.remove(guildId);
	}

}
