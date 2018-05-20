package me.shadorc.shadbot.music;

import java.util.concurrent.ConcurrentHashMap;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.util.Snowflake;

public class GuildMusicManager {

	public static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
	public static final ConcurrentHashMap<Snowflake, GuildMusic> GUILD_MUSIC_MAP = new ConcurrentHashMap<>();

	public static GuildMusic createGuildMusic(Guild guild) {
		GuildMusic guildMusic = new GuildMusic(guild, PLAYER_MANAGER);
		GUILD_MUSIC_MAP.put(guild.getId(), guildMusic);

		guild.getAudioManager().setAudioProvider(guildMusic.getAudioProvider());

		return guildMusic;
	}

}
