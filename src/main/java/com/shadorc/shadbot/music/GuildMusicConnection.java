package com.shadorc.shadbot.music;

import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

public class GuildMusicConnection {

    public enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED;
    }

    private final DiscordClient client;
    private final Snowflake guildId;

    private volatile State state;
    private volatile VoiceConnection voiceConnection;
    private volatile GuildMusic guildMusic;

    public GuildMusicConnection(DiscordClient client, Snowflake guildId) {
        this.client = client;
        this.guildId = guildId;
        this.state = State.DISCONNECTED;
        this.voiceConnection = null;
        this.guildMusic = null;
    }

    /**
     * Requests to join a voice channel.
     */
    public Mono<Void> joinVoiceChannel(Snowflake voiceChannelId, AudioProvider audioProvider) {
        if (this.state != State.DISCONNECTED) {
            return Mono.empty();
        }

        this.changeState(State.CONNECTING);
        MusicManager.LOGGER.debug("{Guild ID: {}} Joining voice channel...", this.guildId.asLong());

        return this.client.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
                .timeout(Config.DEFAULT_TIMEOUT)
                .flatMap(voiceConnection -> {
                    LogUtils.info("{Guild ID: %d} Voice channel joined.", this.guildId.asLong());

                    this.voiceConnection = voiceConnection;
                    this.changeState(State.CONNECTED);

                    // If an error occurred while loading a track, the voice channel can be joined after
                    // the guild music is destroyed. The delay is needed to avoid transition error.
                    return Mono.justOrEmpty(this.guildMusic)
                            .switchIfEmpty(Mono.delay(Duration.ofSeconds(2), Schedulers.elastic())
                                    .then(Mono.fromRunnable(this::leaveVoiceChannel)));
                })
                .onErrorResume(TimeoutException.class, err -> this.onVoiceConnectionTimeout())
                .then();
    }

    private <T> Mono<T> onVoiceConnectionTimeout() {
        LogUtils.info("{Guild ID: %d} Voice connection timed out.", this.guildId.asLong());
        this.changeState(State.DISCONNECTED);
        return Mono.justOrEmpty(this.guildMusic)
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(
                        Emoji.WARNING + " Sorry, I can't join this voice channel right now. "
                                + "Please, try again in a few seconds or with another voice channel.", channel))
                .then(Mono.fromRunnable(this::leaveVoiceChannel));
    }

    /**
     * Leave the voice channel and destroy the {@link GuildMusic}.
     */
    public void leaveVoiceChannel() {
        if (this.voiceConnection != null) {
            this.voiceConnection.disconnect();
            this.voiceConnection = null;
            this.changeState(State.DISCONNECTED);
            MusicManager.LOGGER.info("{Guild ID: {}} Voice channel left.", this.guildId.asLong());
        }

        if (this.guildMusic != null) {
            this.guildMusic.destroy();
            this.guildMusic = null;
            MusicManager.LOGGER.debug("{Guild ID: {}} Guild music destroyed.", this.guildId.asLong());
        }
    }

    public VoiceConnection getVoiceConnection() {
        return this.voiceConnection;
    }

    public GuildMusic getGuildMusic() {
        return this.guildMusic;
    }

    public void setGuildMusic(GuildMusic guildMusic) {
        this.guildMusic = guildMusic;
    }

    public void changeState(State state) {
        MusicManager.LOGGER.debug("{Guild ID: {}} Changing music state to {}.", this.guildId.asLong(), state.toString());
        this.state = state;
    }

}
