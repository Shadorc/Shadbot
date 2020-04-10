package com.shadorc.shadbot.music;

import com.shadorc.shadbot.utils.LogUtils;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class GuildMusicConnection {

    private final GatewayDiscordClient client;
    private final Snowflake guildId;

    @Nullable
    private VoiceConnection voiceConnection;
    @Nullable
    private GuildMusic guildMusic;

    public GuildMusicConnection(GatewayDiscordClient client, Snowflake guildId) {
        this.client = client;
        this.guildId = guildId;
    }

    /**
     * Requests to join a voice channel.
     */
    public Mono<Void> joinVoiceChannel(Snowflake voiceChannelId, AudioProvider audioProvider) {
        // Do not join a voice channel if a voice connection already exists and is connected
        if (this.getVoiceConnection() != null && this.getVoiceConnection().isConnected()) {
            return Mono.empty();
        }

        LOGGER.debug("{Guild ID: {}} Joining voice channel...", this.guildId.asLong());

        return this.client.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
                .flatMap(voiceConnection -> {
                    LogUtils.info("{Guild ID: %d} Voice channel joined.", this.guildId.asLong());

                    this.setVoiceConnection(voiceConnection);

                    // If the voice connection has been disconnected or if an error occurred while loading a track
                    // (guild music being null), the voice channel can be joined after the guild music is destroyed.
                    if (!this.getVoiceConnection().isConnected() || this.getGuildMusic() == null) {
                        return this.leaveVoiceChannel();
                    }
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Leave the voice channel and destroy the {@link GuildMusic}.
     */
    public Mono<Void> leaveVoiceChannel() {
        return Mono.justOrEmpty(this.getVoiceConnection())
                .flatMap(VoiceConnection::disconnect)
                .doOnTerminate(() -> {
                    if (this.getVoiceConnection() != null) {
                        this.setVoiceConnection(null);
                        LOGGER.info("{Guild ID: {}} Voice channel left.", this.guildId.asLong());
                    }

                    if (this.getGuildMusic() != null) {
                        this.getGuildMusic().destroy();
                        this.setGuildMusic(null);
                        LOGGER.debug("{Guild ID: {}} Guild music destroyed.", this.guildId.asLong());
                    }
                });
    }

    @Nullable
    public VoiceConnection getVoiceConnection() {
        return this.voiceConnection;
    }

    @Nullable
    public GuildMusic getGuildMusic() {
        return this.guildMusic;
    }

    public void setGuildMusic(GuildMusic guildMusic) {
        this.guildMusic = guildMusic;
    }

    public void setVoiceConnection(VoiceConnection voiceConnection) {
        this.voiceConnection = voiceConnection;
    }

}
