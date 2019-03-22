package me.shadorc.shadbot.music;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.listener.music.TrackEventListener;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;

public class GuildMusicManager {

	private static final AudioPlayerManager AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
	private static final Map<Snowflake, GuildMusic> GUILD_MUSIC_MAP = new ConcurrentHashMap<>();

	static {
		AUDIO_PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
		AudioSourceManagers.registerRemoteSources(AUDIO_PLAYER_MANAGER);
	}

	/**
	 * Schedules loading a track or playlist with the specified identifier. Items loaded with the same
	 * guild ID are handled sequentially in the order of calls to this method.
	 * 
	 * @return A future for this operation
	 */
	public static Future<Void> loadItemOrdered(Snowflake guildId, String identifier, AudioLoadResultListener listener) {
		return AUDIO_PLAYER_MANAGER.loadItemOrdered(guildId, identifier, listener);
	}

	/**
	 * Get the {@link GuildMusic} corresponding to the provided {@code guildId}. If there is none,
	 * a new one is created and a request to join the {@link VoiceChannel} corresponding to the provided
	 * {@code voiceChannelId} is sent.
	 */
	public static GuildMusic getOrCreate(DiscordClient client, Snowflake guildId, Snowflake voiceChannelId) {
		return GUILD_MUSIC_MAP.computeIfAbsent(guildId,
				ignored -> {
					LogUtils.info("{Guild ID: %d} Creating guild music.", guildId.asLong());
					final AudioPlayer audioPlayer = AUDIO_PLAYER_MANAGER.createPlayer();
					// Listen to track start, end, exception and stuck.
					audioPlayer.addListener(new TrackEventListener(guildId));

					final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, Shadbot.getDatabase().getDBGuild(guildId).getDefaultVol());
					final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);

					final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
					guildMusic.joinVoiceChannel(voiceChannelId, audioProvider)
							.subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));

					return guildMusic;
				});
	}

	public static GuildMusic get(Snowflake guildId) {
		return GUILD_MUSIC_MAP.get(guildId);
	}

	public static GuildMusic remove(Snowflake guildId) {
		LogUtils.info("{Guild ID: %d} Removing guild music.", guildId.asLong());
		return GUILD_MUSIC_MAP.remove(guildId);
	}

	// TODO: Remove, for debug purpose
	public static Map<Snowflake, GuildMusic> get() {
		return Collections.unmodifiableMap(GUILD_MUSIC_MAP);
	}

}
