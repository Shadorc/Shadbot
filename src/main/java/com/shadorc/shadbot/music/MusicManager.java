package com.shadorc.shadbot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
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
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.listener.music.TrackEventListener;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.shadorc.shadbot.listener.VoiceStateUpdateListener.VOICE_COUNT_GAUGE;

public class MusicManager {

    private static MusicManager instance;

    public static final Logger LOGGER = Loggers.getLogger("shadbot.music");

    static {
        MusicManager.instance = new MusicManager();
    }

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Snowflake, GuildMusic> guildMusics;
    private final Map<Snowflake, AtomicBoolean> guildJoining;

    private MusicManager() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        this.guildMusics = new ConcurrentHashMap<>();
        this.guildJoining = new ConcurrentHashMap<>();

        //IPv6 rotation config
        final String ipv6Block = CredentialManager.getInstance().get(Credential.IPV6_BLOCK);
        if (!Config.IS_SNAPSHOT && ipv6Block != null && !ipv6Block.isBlank()) {
            LOGGER.info("Configuring YouTube IP rotator");
            final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(ipv6Block));
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
        return Mono.justOrEmpty(this.getGuildMusic(guildId))
                .switchIfEmpty(Mono.defer(() -> {
                    final AudioPlayer audioPlayer = this.audioPlayerManager.createPlayer();
                    audioPlayer.addListener(new TrackEventListener(guildId));
                    final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);

                    return this.joinVoiceChannel(client, guildId, voiceChannelId, audioProvider)
                            .zipWith(DatabaseManager.getGuilds()
                                    .getDBGuild(guildId)
                                    .map(DBGuild::getSettings)
                                    .map(Settings::getDefaultVol)
                                    .map(volume -> new TrackScheduler(audioPlayer, volume)))
                            .map(tuple -> {
                                final GuildMusic guildMusic = new GuildMusic(client, guildId, tuple.getT1(), tuple.getT2());
                                this.guildMusics.put(guildId, guildMusic);
                                return guildMusic;
                            })
                            .doOnNext(guildMusic -> LOGGER.debug("{Guild ID: {}} Guild music created", guildId.asLong()));
                }));
    }

    /**
     * Requests to join a voice channel.
     */
    private Mono<VoiceConnection> joinVoiceChannel(GatewayDiscordClient client, Snowflake guildId, Snowflake voiceChannelId,
                                                   AudioProvider audioProvider) {
        // Do not join the voice channel if the bot is already joining a voice channel or if a voice connection is already established
        if (this.guildJoining.computeIfAbsent(guildId, ignored -> new AtomicBoolean(false)).getAndSet(true)
                || this.guildMusics.containsKey(guildId)) {
            return Mono.empty();
        }

        LOGGER.info("{Guild ID: {}} Joining voice channel...", guildId.asLong());

        return client.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
                .doOnNext(ignored -> {
                    LOGGER.info("{Guild ID: {}} Voice channel joined", guildId.asLong());
                    VOICE_COUNT_GAUGE.inc();
                })
                .doOnTerminate(() -> this.guildJoining.getOrDefault(guildId, new AtomicBoolean()).set(false));
    }

    public Mono<Void> destroyConnection(Snowflake guildId) {
        this.guildJoining.remove(guildId);
        final GuildMusic guildMusic = this.guildMusics.remove(guildId);
        if (guildMusic != null) {
            guildMusic.destroy();
            LOGGER.debug("{Guild ID: {}} Guild music destroyed.", guildId.asLong());
        }

        return Mono.justOrEmpty(guildMusic)
                .map(GuildMusic::getVoiceConnection)
                .filter(VoiceConnection::isConnected)
                .flatMap(VoiceConnection::disconnect);
    }

    public Optional<GuildMusic> getGuildMusic(Snowflake guildId) {
        final GuildMusic guildMusic = this.guildMusics.get(guildId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{Guild ID: {}} Guild music request: {}", guildId.asLong(), guildMusic);
        }
        return Optional.ofNullable(guildMusic);
    }

    public long getGuildMusicCount() {
        return this.guildMusics.size();
    }

    public static MusicManager getInstance() {
        return MusicManager.instance;
    }
}
