package me.shadorc.shadbot.music;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;

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
        if (!this.state.equals(State.DISCONNECTED)) {
            return Mono.empty();
        }

        this.changeState(State.CONNECTING);
        LogUtils.debug("{Guild ID: %d} Joining voice channel...", this.guildId.asLong());

        return this.client.getChannelById(voiceChannelId)
                .cast(VoiceChannel.class)
                .flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)).elapsed())
                .flatMap(tuple -> {
                    final Long elapsedTime = tuple.getT1();
                    final VoiceConnection voiceConnection = tuple.getT2();

                    LogUtils.info("{Guild ID: %d} Voice channel joined in %dms.", this.guildId.asLong(), elapsedTime);
                    if (elapsedTime > Duration.ofSeconds(10).toMillis()) {
                        LogUtils.warn(Shadbot.getClient(), String.format("{Guild ID: %d} Joining a voice channel took %dms",
                                this.guildId.asLong(), elapsedTime));
                    }

                    this.voiceConnection = voiceConnection;
                    this.changeState(State.CONNECTED);

                    // If an error occurred while loading a track, the voice channel can be joined after
                    // the guild music is destroyed. The delay is needed to avoid transition error.
                    return Mono.justOrEmpty(this.guildMusic)
                            .switchIfEmpty(Mono.delay(Duration.ofSeconds(2))
                                    .then(Mono.fromRunnable(this::leaveVoiceChannel)));
                })
                .then();
    }

    /**
     * Leave the voice channel and destroy the {@link GuildMusic}.
     */
    public void leaveVoiceChannel() {
        if (this.voiceConnection != null) {
            this.voiceConnection.disconnect();
            this.voiceConnection = null;
            this.changeState(State.DISCONNECTED);
            LogUtils.info("{Guild ID: %d} Voice channel left.", this.guildId.asLong());
        }

        if (this.guildMusic != null) {
            this.guildMusic.destroy();
            this.guildMusic = null;
            LogUtils.debug("{Guild ID: %d} Guild music destroyed.", this.guildId.asLong());
        }
    }

    public GuildMusic getGuildMusic() {
        return this.guildMusic;
    }

    public void setGuildMusic(GuildMusic guildMusic) {
        this.guildMusic = guildMusic;
    }

    public void changeState(State state) {
        LogUtils.debug("{Guild ID: %d} Changing music state to %s.", this.guildId.asLong(), state.toString());
        this.state = state;
    }

}
