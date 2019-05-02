package me.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.object.VoiceState;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.listener.music.TrackEventListener;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MusicManager {

    private static final AudioPlayerManager AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
    public static final Map<Snowflake, GuildMusicConnection> GUILD_MUSIC_CONNECTIONS = new ConcurrentHashMap<>();

    static {
        AUDIO_PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(AUDIO_PLAYER_MANAGER);
        MusicManager.startWatcher();
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
        final GuildMusicConnection guildMusicConnection = GUILD_MUSIC_CONNECTIONS.computeIfAbsent(guildId,
                ignored -> {
                    LogUtils.debug("{Guild ID: %d} Creating guild music connection.", guildId.asLong());
                    return new GuildMusicConnection(client, guildId);
                });

        if (guildMusicConnection.getGuildMusic() == null) {
            LogUtils.debug("{Guild ID: %d} Creating guild music.", guildId.asLong());
            final AudioPlayer audioPlayer = AUDIO_PLAYER_MANAGER.createPlayer();
            audioPlayer.addListener(new TrackEventListener(guildId));

            final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, Shadbot.getDatabase().getDBGuild(guildId).getDefaultVol());
            final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);
            guildMusicConnection.setGuildMusic(guildMusic);

            final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
            guildMusicConnection.joinVoiceChannel(voiceChannelId, audioProvider)
                    .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
        }

        return guildMusicConnection.getGuildMusic();
    }

    public static GuildMusicConnection getConnection(Snowflake guildId) {
        return GUILD_MUSIC_CONNECTIONS.get(guildId);
    }

    public static GuildMusic getMusic(Snowflake guildId) {
        final GuildMusicConnection guildMusicConnection = MusicManager.getConnection(guildId);
        if (guildMusicConnection == null) {
            return null;
        }
        return guildMusicConnection.getGuildMusic();
    }

    public static void removeConnection(Snowflake guildId) {
        final GuildMusicConnection guildMusicConnection = GUILD_MUSIC_CONNECTIONS.remove(guildId);
        if (guildMusicConnection != null) {
            guildMusicConnection.leaveVoiceChannel();
        }
    }

    private static void startWatcher() {
        Flux.interval(Duration.ofMinutes(5), Duration.ofMinutes(5))
                .flatMap(ignored -> MusicManager.getGuildIdsWithVoice())
                .doOnNext(guildIdsWithVoice -> {
                    List<Snowflake> diffList = new ArrayList<>(guildIdsWithVoice);
                    diffList.removeAll(MusicManager.getGuildIdsWithGuildMusics());
                    if (!diffList.isEmpty()) {
                        LogUtils.warn(Shadbot.getClient(), String.format("Desynchronization detected: %s", diffList.toString()));
                    }
                })
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
    }

    public static List<Snowflake> getGuildIdsWithGuildMusics() {
        return GUILD_MUSIC_CONNECTIONS.keySet().stream()
                .filter(guildId -> MusicManager.getConnection(guildId).getGuildMusic() != null)
                .collect(Collectors.toList());
    }

    public static Mono<List<Snowflake>> getGuildIdsWithVoice() {
        return Shadbot.getClient().getGuilds()
                .flatMap(guild -> guild.getMemberById(guild.getClient().getSelfId().get()))
                .flatMap(Member::getVoiceState)
                .flatMap(VoiceState::getChannel)
                .map(VoiceChannel::getGuildId)
                .collectList();
    }

}
