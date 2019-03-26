package me.shadorc.shadbot.music;

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

public class GuildMusicStateManager {

	private static final AudioPlayerManager AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
	private static final Map<Snowflake, GuildMusicState> GUILD_MUSIC_STATES = new ConcurrentHashMap<>();

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
		final GuildMusicState guildMusicState = GUILD_MUSIC_STATES.computeIfAbsent(guildId,
				ignored -> {
					LogUtils.debug("{Guild ID: %d} Creating guild music state.", guildId.asLong());
					return new GuildMusicState(client, guildId);
				});

		if(guildMusicState.getGuildMusic() == null) {
			LogUtils.debug("{Guild ID: %d} Creating guild music.", guildId.asLong());
			final AudioPlayer audioPlayer = AUDIO_PLAYER_MANAGER.createPlayer();
			audioPlayer.addListener(new TrackEventListener(guildId));

			final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, Shadbot.getDatabase().getDBGuild(guildId).getDefaultVol());
			final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);
			guildMusicState.setGuildMusic(guildMusic);

			final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
			guildMusicState.joinVoiceChannel(voiceChannelId, audioProvider)
					.subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
		}

		return guildMusicState.getGuildMusic();
	}

	public static GuildMusicState getState(Snowflake guildId) {
		return GUILD_MUSIC_STATES.get(guildId);
	}

	public static GuildMusic getMusic(Snowflake guildId) {
		final GuildMusicState guildMusicState = GuildMusicStateManager.getState(guildId);
		if(guildMusicState == null) {
			return null;
		}
		return guildMusicState.getGuildMusic();
	}

	public static void removeState(Snowflake guildId) {
		final GuildMusicState guildMusicState = GUILD_MUSIC_STATES.remove(guildId);
		if(guildMusicState != null) {
			guildMusicState.leaveVoiceChannel();
		}
	}

	// TODO: Remove, for debug purpose
	public static long count() {
		return GUILD_MUSIC_STATES.values().stream()
				.filter(state -> state.getGuildMusic() != null)
				.count();
	}

}
