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
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.listener.music.AudioLoadResultListener;
import com.shadorc.shadbot.listener.music.TrackEventListener;
import com.shadorc.shadbot.utils.LogUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.util.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MusicManager {

    public static final Logger LOGGER = LogUtil.getLogger(LogUtil.Category.MUSIC);

    private static final AudioPlayerManager AUDIO_PLAYER_MANAGER;
    private static final Map<Snowflake, GuildMusic> GUILD_MUSIC_MAP;
    private static final Map<Snowflake, AtomicBoolean> GUILD_JOINING;

    static {
        AUDIO_PLAYER_MANAGER = new DefaultAudioPlayerManager();
        AUDIO_PLAYER_MANAGER.getConfiguration().setFrameBufferFactory(NonAllocatingAudioFrameBuffer::new);
        AUDIO_PLAYER_MANAGER.getConfiguration().setFilterHotSwapEnabled(true);
        AudioSourceManagers.registerRemoteSources(AUDIO_PLAYER_MANAGER);

        GUILD_MUSIC_MAP = new ConcurrentHashMap<>();
        GUILD_JOINING = new ConcurrentHashMap<>();

        //IPv6 rotation configuration
        final String ipv6Block = CredentialManager.get(Credential.IPV6_BLOCK);
        if (ipv6Block != null && !Config.IS_SNAPSHOT) {
            LOGGER.info("Configuring YouTube IP rotator");
            @SuppressWarnings("rawtypes") final List<IpBlock> blocks = Collections.singletonList(new Ipv6Block(ipv6Block));
            final AbstractRoutePlanner planner = new RotatingNanoIpRoutePlanner(blocks);

            new YoutubeIpRotatorSetup(planner)
                    .forSource(AUDIO_PLAYER_MANAGER.source(YoutubeAudioSourceManager.class))
                    .setup();
        }
    }

    /**
     * Schedules loading a track or playlist with the specified identifier. Items loaded with the same
     * guild ID are handled sequentially in the order of calls to this method.
     *
     * @return A future for this operation.
     */
    protected static Future<Void> loadItemOrdered(long guildId, AudioLoadResultListener listener) {
        return AUDIO_PLAYER_MANAGER.loadItemOrdered(guildId, listener.getIdentifier(), listener);
    }

    /**
     * Gets the {@link GuildMusic} corresponding to the provided {@code guildId}. If there is none,
     * a new one is created and a request to join the {@link VoiceChannel} corresponding to the provided
     * {@code voiceChannelId} is sent.
     */
    public static Mono<GuildMusic> getOrCreate(GatewayDiscordClient gateway, Locale locale, Snowflake guildId,
                                               Snowflake voiceChannelId) {
        return Mono.justOrEmpty(MusicManager.getGuildMusic(guildId))
                .switchIfEmpty(Mono.defer(() -> {
                    final AudioPlayer audioPlayer = AUDIO_PLAYER_MANAGER.createPlayer();
                    audioPlayer.addListener(new TrackEventListener(locale, guildId));
                    final LavaplayerAudioProvider audioProvider = new LavaplayerAudioProvider(audioPlayer);

                    return MusicManager.joinVoiceChannel(gateway, guildId, voiceChannelId, audioProvider)
                            .flatMap(voiceConnection -> DatabaseManager.getGuilds().getSettings(voiceConnection.getGuildId()))
                            .map(settings -> settings.getDefaultVol().orElse(Config.DEFAULT_VOLUME))
                            .map(volume -> new TrackScheduler(audioPlayer, volume))
                            .map(trackScheduler -> new GuildMusic(gateway, guildId, trackScheduler))
                            .doOnNext(guildMusic -> {
                                LOGGER.debug("{Guild ID: {}} Guild music created", guildId.asString());
                                GUILD_MUSIC_MAP.put(guildId, guildMusic);
                            });
                }));
    }

    /**
     * Requests to join a voice channel.
     */
    private static Mono<VoiceConnection> joinVoiceChannel(GatewayDiscordClient gateway, Snowflake guildId, Snowflake voiceChannelId,
                                                          AudioProvider audioProvider) {
        // Do not join the voice channel if the bot is already joining one
        if (GUILD_JOINING.computeIfAbsent(guildId, id -> new AtomicBoolean()).getAndSet(true)) {
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
                // Do not join the voice channel if the current voice connection is not disconnected
                .filterWhen(__ -> isDisconnected)
                .doOnNext(__ -> LOGGER.info("{Guild ID: {}} Joining voice channel...", guildId.asString()))
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
                .doOnTerminate(() -> GUILD_JOINING.remove(guildId));
    }

    public static Mono<Void> destroyConnection(Snowflake guildId) {
        final GuildMusic guildMusic = GUILD_MUSIC_MAP.remove(guildId);
        if (guildMusic != null) {
            guildMusic.destroy();
            LOGGER.debug("{Guild ID: {}} Guild music destroyed", guildId.asString());
        }

        return Mono.justOrEmpty(guildMusic)
                .doOnEach(it -> LOGGER.info("{Guild ID: {}} justOrEmpty: {}", guildId.asString(), it))
                .map(GuildMusic::getGateway)
                .doOnEach(it -> LOGGER.info("{Guild ID: {}} getGateway: {}", guildId.asString(), it))
                .map(GatewayDiscordClient::getVoiceConnectionRegistry)
                .doOnEach(it -> LOGGER.info("{Guild ID: {}} getVoiceConnectionRegistry: {}" , guildId.asString(), it))
                .flatMap(registry -> registry.getVoiceConnection(guildId))
                .doOnEach(it -> LOGGER.info("{Guild ID: {}} getVoiceConnection: {}" , guildId.asString(), it))
                .flatMap(VoiceConnection::disconnect)
                .log("%s:destroyConnection".formatted(guildId.asString()));
    }

    public static Optional<GuildMusic> getGuildMusic(Snowflake guildId) {
        final GuildMusic guildMusic = GUILD_MUSIC_MAP.get(guildId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("{Guild ID: {}} Guild music request: {}", guildId.asString(), guildMusic);
        }
        return Optional.ofNullable(guildMusic);
    }

}
