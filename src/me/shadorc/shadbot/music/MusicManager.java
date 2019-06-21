package me.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.database.DatabaseManager;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.listener.music.TrackEventListener;
import me.shadorc.shadbot.utils.ExceptionHandler;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MusicManager {

    private static MusicManager instance;

    static {
        MusicManager.instance = new MusicManager();
    }

    public static final Logger LOGGER = Loggers.getLogger("shadbot.music");

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Snowflake, GuildMusicConnection> guildMusicConnections;

    private MusicManager() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        this.guildMusicConnections = new ConcurrentHashMap<>();

    }

    /**
     * Schedules loading a track or playlist with the specified identifier. Items loaded with the same
     * guild ID are handled sequentially in the order of calls to this method.
     *
     * @return A future for this operation
     */
    public Future<Void> loadItemOrdered(Snowflake guildId, String identifier, AudioLoadResultListener listener) {
        return this.audioPlayerManager.loadItemOrdered(guildId, identifier, listener);
    }

    /**
     * Get the {@link GuildMusic} corresponding to the provided {@code guildId}. If there is none,
     * a new one is created and a request to join the {@link VoiceChannel} corresponding to the provided
     * {@code voiceChannelId} is sent.
     */
    public GuildMusic getOrCreate(DiscordClient client, Snowflake guildId, Snowflake voiceChannelId) {
        final GuildMusicConnection guildMusicConnection = this.guildMusicConnections.computeIfAbsent(guildId,
                ignored -> {
                    LOGGER.debug("{Guild ID: {}} Creating guild music connection.", guildId.asLong());
                    return new GuildMusicConnection(client, guildId);
                });

        if (guildMusicConnection.getGuildMusic() == null) {
            LOGGER.debug("{Guild ID: {}} Creating guild music.", guildId.asLong());
            final AudioPlayer audioPlayer = this.audioPlayerManager.createPlayer();
            audioPlayer.addListener(new TrackEventListener(guildId));

            final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, DatabaseManager.getInstance().getDBGuild(guildId).getDefaultVol());
            final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);
            guildMusicConnection.setGuildMusic(guildMusic);

            final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
            guildMusicConnection.joinVoiceChannel(voiceChannelId, audioProvider)
                    .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
        }

        return guildMusicConnection.getGuildMusic();
    }

    public GuildMusicConnection getConnection(Snowflake guildId) {
        return this.guildMusicConnections.get(guildId);
    }

    public GuildMusic getMusic(Snowflake guildId) {
        final GuildMusicConnection guildMusicConnection = this.getConnection(guildId);
        if (guildMusicConnection == null) {
            return null;
        }
        return guildMusicConnection.getGuildMusic();
    }

    public void removeConnection(Snowflake guildId) {
        final GuildMusicConnection guildMusicConnection = this.guildMusicConnections.remove(guildId);
        if (guildMusicConnection != null) {
            guildMusicConnection.leaveVoiceChannel();
        }
    }

    public List<Snowflake> getGuildIdsWithGuildMusics() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).getGuildMusic() != null)
                .collect(Collectors.toList());
    }

    public List<Snowflake> getGuildIdsWithVoice() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).getVoiceConnection() != null)
                .collect(Collectors.toList());
    }

    public static MusicManager getInstance() {
        return MusicManager.instance;
    }

}
