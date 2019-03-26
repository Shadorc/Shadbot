package me.shadorc.shadbot.music;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class GuildMusicState {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final AtomicBoolean isConnecting;
	private volatile VoiceConnection voiceConnection;
	private volatile GuildMusic guildMusic;

	public GuildMusicState(DiscordClient client, Snowflake guildId) {
		this.client = client;
		this.guildId = guildId;
		this.isConnecting = new AtomicBoolean(false);
		this.voiceConnection = null;
		this.guildMusic = null;
	}

	/**
	 * Requests to join a voice channel.
	 */
	public Mono<Void> joinVoiceChannel(Snowflake voiceChannelId, AudioProvider audioProvider) {
		return this.client.getChannelById(voiceChannelId)
				.cast(VoiceChannel.class)
				.filter(ignored -> !this.isConnecting.get() && this.voiceConnection == null)
				.doOnNext(ignored -> {
					LogUtils.debug("{Guild ID: %d} Joining voice channel...", this.guildId.asLong());
					this.isConnecting.set(true);
				})
				.flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider))
						.timeout(Duration.ofSeconds(Config.DEFAULT_TIMEOUT)))
				.onErrorResume(TimeoutException.class, err -> {
					LogUtils.info("{Guild ID: %d} Voice connection timed out.", this.guildId.asLong());
					if(this.guildMusic != null) {
						return this.guildMusic.getMessageChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(
										Emoji.WARNING + " Sorry, I can't join this voice channel right now. "
												+ "Please retry in a few minutes or with another voice channel.", channel))
								.then(Mono.fromRunnable(this::leaveVoiceChannel));
					}
					return Mono.empty();
				})
				.flatMap(voiceConnection -> {
					LogUtils.info("{Guild ID: %d} Voice channel joined.", this.guildId.asLong());
					this.voiceConnection = voiceConnection;

					// If an error occurred while loading a track, the voice channel can be joined after
					// the guild music is destroyed. The delay is needed to avoid transition error.
					if(this.guildMusic == null) {
						return Mono.delay(Duration.ofSeconds(2))
								.doOnNext(ignored -> this.leaveVoiceChannel());
					}
					return Mono.empty();
				})
				.doOnTerminate(() -> this.isConnecting.set(false))
				.then();
	}

	/**
	 * Leave the voice channel and destroy the {@link GuildMusic}.
	 */
	public void leaveVoiceChannel() {
		if(this.voiceConnection != null) {
			this.voiceConnection.disconnect();
			this.voiceConnection = null;
			LogUtils.info("{Guild ID: %d} Voice channel left.", this.guildId.asLong());
		}

		if(this.guildMusic != null) {
			this.guildMusic.destroy();
			this.guildMusic = null;
		}
	}

	public GuildMusic getGuildMusic() {
		return this.guildMusic;
	}

	public void setGuildMusic(GuildMusic guildMusic) {
		this.guildMusic = guildMusic;
	}

}
