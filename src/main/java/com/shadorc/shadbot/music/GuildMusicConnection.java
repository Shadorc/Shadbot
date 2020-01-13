package com.shadorc.shadbot.music;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class GuildMusicConnection {

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private final GatewayDiscordClient client;
    private final Snowflake guildId;

    private final AtomicReference<State> state;
    private final AtomicReference<VoiceConnection> voiceConnection;
    private final AtomicReference<GuildMusic> guildMusic;

    public GuildMusicConnection(GatewayDiscordClient client, Snowflake guildId) {
        this.client = client;
        this.guildId = guildId;

        this.state = new AtomicReference<>(State.DISCONNECTED);
        this.voiceConnection = new AtomicReference<>();
        this.guildMusic = new AtomicReference<>();
    }

    /**
     * Requests to join a voice channel.
     */
    public Mono<Void> joinVoiceChannel(Snowflake voiceChannelId, AudioProvider audioProvider) {
        if (this.state.get() != State.DISCONNECTED) {
            return Mono.empty();
        }

        this.changeState(State.CONNECTING);
        LOGGER.debug("{Guild ID: {}} Joining voice channel...", this.guildId.asLong());

        return this.client.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider))
                        .publishOn(Schedulers.boundedElastic()))
                .timeout(Config.TIMEOUT)
                .flatMap(voiceConnection -> {
                    LogUtils.info("{Guild ID: %d} Voice channel joined.", this.guildId.asLong());

                    this.voiceConnection.set(voiceConnection);
                    this.changeState(State.CONNECTED);

                    // If an error occurred while loading a track, the voice channel can be joined after
                    // the guild music is destroyed. The delay is needed to avoid transition error.
                    return Mono.justOrEmpty(this.getGuildMusic())
                            .switchIfEmpty(Mono.delay(Duration.ofSeconds(3), Schedulers.boundedElastic())
                                    .then(Mono.fromRunnable(this::leaveVoiceChannel)));
                })
                .then()
                .onErrorResume(TimeoutException.class, err -> this.onVoiceConnectionTimeout());
    }

    private Mono<Void> onVoiceConnectionTimeout() {
        LogUtils.info("{Guild ID: %d} Voice connection timed out.", this.guildId.asLong());
        this.changeState(State.DISCONNECTED);
        return Mono.justOrEmpty(this.getGuildMusic())
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(
                        Emoji.WARNING + " Sorry, I can't join this voice channel right now. "
                                + "Please, try again in a few seconds or with another voice channel.", channel))
                .and(Mono.fromRunnable(this::leaveVoiceChannel));
    }

    /**
     * Leave the voice channel and destroy the {@link GuildMusic}.
     */
    public void leaveVoiceChannel() {
        if (this.getVoiceConnection() != null) {
            this.getVoiceConnection().disconnect();
            this.voiceConnection.set(null);
            this.changeState(State.DISCONNECTED);
            MusicManager.LOGGER.info("{Guild ID: {}} Voice channel left.", this.guildId.asLong());
        }

        if (this.getGuildMusic() != null) {
            this.getGuildMusic().destroy();
            this.setGuildMusic(null);
            MusicManager.LOGGER.debug("{Guild ID: {}} Guild music destroyed.", this.guildId.asLong());
        }
    }

    public VoiceConnection getVoiceConnection() {
        return this.voiceConnection.get();
    }

    public GuildMusic getGuildMusic() {
        return this.guildMusic.get();
    }

    public void setGuildMusic(GuildMusic guildMusic) {
        this.guildMusic.set(guildMusic);
    }

    public void changeState(State state) {
        MusicManager.LOGGER.debug("{Guild ID: {}} Changing music state to {}.", this.guildId.asLong(), state.toString());
        this.state.set(state);
    }

}
