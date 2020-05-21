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
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.db.guilds.entity.Settings;
import com.shadorc.shadbot.listener.music.TrackEventListener;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import discord4j.voice.retry.VoiceGatewayException;
import io.prometheus.client.Gauge;
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

public class MusicManager {

    public static final Logger LOGGER = Loggers.getLogger("shadbot.music");
    private static final Gauge GUILD_MUSIC_GAUGE = Gauge.build().namespace("shadbot").name("guild_music_count")
            .help("Guild music count").register();

    private static MusicManager instance;

    static {
        MusicManager.instance = new MusicManager();
    }

    private final AudioPlayerManager audioPlayerManager;
    private final Map<Snowflake, GuildMusic> guildMusics;
    private final Map<Snowflake, AtomicBoolean> guildJoining;

    private MusicManager() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        this.audioPlayerManager.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        this.audioPlayerManager.getConfiguration().setFilterHotSwapEnabled(true);
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
    protected Future<Void> loadItemOrdered(long guildId, String identifier, AudioLoadResultHandler listener) {
        return this.audioPlayerManager.loadItemOrdered(guildId, identifier, listener);
    }

    /**
     * Gets the {@link GuildMusic} corresponding to the provided {@code guildId}. If there is none,
     * a new one is created and a request to join the {@link VoiceChannel} corresponding to the provided
     * {@code voiceChannelId} is sent.
     */
    public Mono<GuildMusic> getOrCreate(GatewayDiscordClient gateway, Snowflake guildId, Snowflake voiceChannelId) {
        return Mono.justOrEmpty(this.getGuildMusic(guildId))
                .switchIfEmpty(Mono.defer(() -> {
                    final AudioPlayer audioPlayer = this.audioPlayerManager.createPlayer();
                    audioPlayer.addListener(new TrackEventListener(guildId));
                    final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);

                    return this.joinVoiceChannel(gateway, guildId, voiceChannelId, audioProvider)
                            .flatMap(ignored -> DatabaseManager.getGuilds().getDBGuild(guildId))
                            .map(DBGuild::getSettings)
                            .map(Settings::getDefaultVol)
                            .map(volume -> new TrackScheduler(audioPlayer, volume))
                            .map(trackScheduler -> new GuildMusic(gateway, guildId, trackScheduler))
                            .doOnNext(guildMusic -> {
                                this.guildMusics.put(guildId, guildMusic);
                                GUILD_MUSIC_GAUGE.inc();
                                LOGGER.debug("{Guild ID: {}} Guild music created", guildId.asLong());
                            });
                }));
    }

    /**
     * Requests to join a voice channel.
     */
    private Mono<VoiceConnection> joinVoiceChannel(GatewayDiscordClient gateway, Snowflake guildId, Snowflake voiceChannelId,
                                                   AudioProvider audioProvider) {

        // Do not join the voice channel if the bot is already joining one
        if (this.guildJoining.computeIfAbsent(guildId, id -> new AtomicBoolean()).getAndSet(true)) {
            return Mono.empty();
        }

        final Mono<Boolean> isDisconnected = gateway.getVoiceConnectionRegistry()
                .getVoiceConnection(guildId)
                .flatMapMany(VoiceConnection::stateEvents)
                .next()
                .map(VoiceConnection.State.DISCONNECTED::equals)
                .defaultIfEmpty(true);

        return gateway.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                // Do not join the voice channel if the current voice connection is in not disconnected
                .filterWhen(ignored -> isDisconnected)
                .doOnNext(ignored -> LOGGER.info("{Guild ID: {}} Joining voice channel...", guildId.asLong()))
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
                .doOnError(VoiceGatewayException.class, err -> LOGGER.warn(err.getMessage()))
                .onErrorMap(VoiceGatewayException.class,
                        err -> new CommandException("An unknown error occurred while joining the voice channel, please try again."))
                .doOnTerminate(() -> this.guildJoining.remove(guildId));
    }

    public Mono<Void> destroyConnection(Snowflake guildId) {
        final GuildMusic guildMusic = this.guildMusics.remove(guildId);
        if (guildMusic != null) {
            guildMusic.destroy();
            GUILD_MUSIC_GAUGE.dec();
            LOGGER.debug("{Guild ID: {}} Guild music destroyed", guildId.asLong());
        }

        return Mono.justOrEmpty(guildMusic)
                .map(GuildMusic::getGateway)
                .map(GatewayDiscordClient::getVoiceConnectionRegistry)
                .flatMap(registry -> registry.getVoiceConnection(guildId))
                .flatMap(VoiceConnection::disconnect);
    }

    public Optional<GuildMusic> getGuildMusic(Snowflake guildId) {
        final GuildMusic guildMusic = this.guildMusics.get(guildId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{Guild ID: {}} Guild music request: {}", guildId.asLong(), guildMusic);
        }
        return Optional.ofNullable(guildMusic);
    }

    public static MusicManager getInstance() {
        return MusicManager.instance;
    }
}
