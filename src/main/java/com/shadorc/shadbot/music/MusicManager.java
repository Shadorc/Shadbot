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
import reactor.util.annotation.Nullable;

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
        return Mono.just(this.guildMusicConnections.computeIfAbsent(guildId,
                ignored -> {
                    LOGGER.debug("{Guild ID: {}} Creating guild music connection.", guildId.asLong());
                    return new GuildMusicConnection(client, guildId);
                }))
                .flatMap(guildMusicConnection -> {
                    if (guildMusicConnection.getGuildMusic().isEmpty()) {
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
                    } else {
                        return Mono.justOrEmpty(guildMusicConnection.getGuildMusic());
                    }
                });
    }

    @Nullable
    public GuildMusicConnection getConnection(Snowflake guildId) {
        return this.guildMusicConnections.get(guildId);
    }

    @Nullable
    public GuildMusic getMusic(Snowflake guildId) {
        return Optional.ofNullable(this.getConnection(guildId))
                .flatMap(GuildMusicConnection::getGuildMusic)
                .orElse(null);
    }

    public Mono<Void> removeConnection(Snowflake guildId) {
        return Mono.justOrEmpty(this.guildMusicConnections.remove(guildId))
                .flatMap(GuildMusicConnection::leaveVoiceChannel);
    }

    public List<Snowflake> getGuildIdsWithGuildMusics() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).getGuildMusic().isPresent())
                .collect(Collectors.toList());
    }

    public List<Snowflake> getGuildIdsWithVoice() {
        return this.guildMusicConnections.keySet().stream()
                .filter(guildId -> this.getConnection(guildId).getVoiceConnection().isPresent())
                .collect(Collectors.toList());
    }

    public static MusicManager getInstance() {
        return MusicManager.instance;
    }

}
