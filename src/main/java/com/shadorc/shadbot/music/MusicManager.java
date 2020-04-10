package com.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.listener.music.TrackEventListener;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class MusicManager {

    private static MusicManager instance;

    public static final Logger LOGGER = Loggers.getLogger("shadbot.music");

    static {
        MusicManager.instance = new MusicManager();
    }

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
     * @return A future for this operation.
     */
    public Future<Void> loadItemOrdered(Snowflake guildId, String identifier, AudioLoadResultHandler listener) {
        return this.audioPlayerManager.loadItemOrdered(guildId, identifier, listener);
    }

    /**
     * Gets the {@link GuildMusic} corresponding to the provided {@code guildId}. If there is none,
     * a new one is created and a request to join the {@link VoiceChannel} corresponding to the provided
     * {@code voiceChannelId} is sent.
     */
    public Mono<GuildMusic> getOrCreate(GatewayDiscordClient client, Snowflake guildId, Snowflake voiceChannelId) {
        final GuildMusicConnection guildMusicConnection = this.guildMusicConnections.computeIfAbsent(guildId,
                id -> {
                    LOGGER.debug("{Guild ID: {}} Creating guild music connection.", id.asLong());
                    return new GuildMusicConnection(client, id);
                });

        return Mono.justOrEmpty(guildMusicConnection.getGuildMusic())
                .switchIfEmpty(Mono.defer(() -> {
                    LOGGER.debug("{Guild ID: {}} Creating guild music.", guildId.asLong());

                    final AudioPlayer audioPlayer = this.audioPlayerManager.createPlayer();
                    audioPlayer.addListener(new TrackEventListener(guildId));

                    return DatabaseManager.getGuilds()
                            .getDBGuild(guildId)
                            .map(DBGuild::getSettings)
                            .map(Settings::getDefaultVol)
                            .flatMap(volume -> {
                                final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, volume);
                                final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);
                                guildMusicConnection.setGuildMusic(guildMusic);

                                final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
                                return guildMusicConnection.joinVoiceChannel(voiceChannelId, audioProvider)
                                        .then(Mono.justOrEmpty(guildMusicConnection.getGuildMusic()));
                            });
                }));
    }

    public Optional<GuildMusicConnection> getConnection(Snowflake guildId) {
        return Optional.ofNullable(this.guildMusicConnections.get(guildId));
    }

    public Optional<GuildMusic> getMusic(Snowflake guildId) {
        return this.getConnection(guildId)
                .flatMap(GuildMusicConnection::getGuildMusic);
    }

    public Mono<Void> removeConnection(Snowflake guildId) {
        return Mono.justOrEmpty(this.guildMusicConnections.remove(guildId))
                .flatMap(GuildMusicConnection::leaveVoiceChannel);
    }

    public List<Snowflake> getGuildIdsWithGuildMusics() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).map(GuildMusicConnection::getGuildMusic).isPresent())
                .collect(Collectors.toList());
    }

    public List<Snowflake> getGuildIdsWithVoice() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).map(GuildMusicConnection::getVoiceConnection).isPresent())
                .collect(Collectors.toList());
    }

    public static MusicManager getInstance() {
        return MusicManager.instance;
    }

}
