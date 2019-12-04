package com.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.playback.NonAllocatingAudioFrameBuffer;
import com.sedmelluq.lava.extensions.youtuberotator.YoutubeIpRotatorSetup;
import com.sedmelluq.lava.extensions.youtuberotator.planner.AbstractRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.planner.RotatingNanoIpRoutePlanner;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.IpBlock;
import com.sedmelluq.lava.extensions.youtuberotator.tools.ip.Ipv6Block;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.listener.music.TrackEventListener;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public final class MusicManager {

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

        //IPv6 rotation config
        if (Config.IPV6_BLOCK != null && !Config.IPV6_BLOCK.isEmpty()) {
            LOGGER.info("Configuring YouTube IP rotator.");
            final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(Config.IPV6_BLOCK));
            final AbstractRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);

            new YoutubeIpRotatorSetup(planner)
                    .forSource(this.audioPlayerManager.source(YoutubeAudioSourceManager.class))
                    .setup();
        }
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
    public Mono<GuildMusic> getOrCreate(DiscordClient client, Snowflake guildId, Snowflake voiceChannelId) {
        return Mono.just(this.guildMusicConnections.computeIfAbsent(guildId,
                ignored -> {
                    LOGGER.debug("{Guild ID: {}} Creating guild music connection.", guildId.asLong());
                    return new GuildMusicConnection(client, guildId);
                }))
                .flatMap(guildMusicConnection -> {
                    if (guildMusicConnection.getGuildMusic() == null) {
                        LOGGER.debug("{Guild ID: {}} Creating guild music.", guildId.asLong());
                        final AudioPlayer audioPlayer = this.audioPlayerManager.createPlayer();
                        audioPlayer.addListener(new TrackEventListener(guildId));

                        final Settings settings = DatabaseManager.getGuilds().getDBGuild(guildId).getSettings();
                        final TrackScheduler trackScheduler = new TrackScheduler(audioPlayer, settings.getDefaultVol());
                        final GuildMusic guildMusic = new GuildMusic(client, guildId, trackScheduler);
                        guildMusicConnection.setGuildMusic(guildMusic);

                        final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);
                        return guildMusicConnection.joinVoiceChannel(voiceChannelId, audioProvider)
                                .thenReturn(guildMusicConnection.getGuildMusic());
                    }

                    return Mono.just(guildMusicConnection.getGuildMusic());
                });
    }

    public GuildMusicConnection getConnection(Snowflake guildId) {
        return this.guildMusicConnections.get(guildId);
    }

    @Nullable
    public GuildMusic getMusic(Snowflake guildId) {
        final GuildMusicConnection guildMusicConnection = this.getConnection(guildId);
        return guildMusicConnection == null ? null : guildMusicConnection.getGuildMusic();
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
