package me.shadorc.shadbot.music;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final TrackScheduler trackScheduler;

	private final Map<AudioLoadResultListener, Future<Void>> listeners;
	private final AtomicBoolean isJoiningVoiceChannel;
	private final AtomicBoolean isWaitingForChoice;
	private final AtomicBoolean isLeavingScheduled;

	private volatile VoiceConnection voiceConnection;
	private volatile Snowflake messageChannelId;
	private volatile Snowflake djId;

	public GuildMusic(DiscordClient client, Snowflake guildId, TrackScheduler trackScheduler) {
		this.client = client;
		this.guildId = guildId;
		this.trackScheduler = trackScheduler;

		this.listeners = new ConcurrentHashMap<>();
		this.isJoiningVoiceChannel = new AtomicBoolean(false);
		this.isWaitingForChoice = new AtomicBoolean(false);
		this.isLeavingScheduled = new AtomicBoolean(false);
	}

	/**
	 * Requests to join a voice channel.
	 * This function does nothing if the bot is already joining a voice channel or is already in a
	 * voice channel.
	 */
	public Mono<Void> joinVoiceChannel(Snowflake voiceChannelId, AudioProvider audioProvider) {
		return this.client.getChannelById(voiceChannelId)
				.cast(VoiceChannel.class)
				.filter(ignored -> !this.isJoiningVoiceChannel.get() && this.voiceConnection == null)
				.doOnNext(ignored -> this.isJoiningVoiceChannel.set(true))
				.doOnNext(ignored -> LogUtils.info("{Guild ID: %d} Joining voice channel...", this.guildId.asLong()))
				.flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(audioProvider)))
				.doOnNext(voiceConnection -> {
					LogUtils.info("{Guild ID: %d} Voice channel joined.", this.guildId.asLong());
					this.voiceConnection = voiceConnection;
				})
				.doOnTerminate(() -> this.isJoiningVoiceChannel.set(false))
				// If a music fails to load directly after creating a guild music, it will be instantly deleted.
				// In this case, it is possible that the voice connection is established after the guild music destruction.
				// To avoid this, if the guild music does not exist when a voice connection is established,
				// disconnect the bot from this voice channel after 5 seconds. This delay is used to avoid UnhandledTransitionException.
				.then(Mono.just(GuildMusicManager.get(this.guildId) == null
						&& !this.isJoiningVoiceChannel.get() && this.voiceConnection != null)
						.filter(Boolean.TRUE::equals)
						.flatMap(ignored -> Mono.delay(Duration.ofSeconds(5)))
						.doOnNext(ignored -> this.voiceConnection.disconnect()))
				.then();
	}

	/**
	 * Leave the voice channel and destroy this {@link GuildMusic}
	 */
	public void leaveVoiceChannel() {
		LogUtils.info("{Guild ID: %d} Leaving voice channel...", guildId.asLong());
		if(this.voiceConnection != null) {
			LogUtils.info("{Guild ID: %d} Actually leaving voice channel.", guildId.asLong());
			this.voiceConnection.disconnect();
			this.voiceConnection = null;
			LogUtils.info("{Guild ID: %d} Voice channel left.", this.guildId.asLong());
		}
		this.destroy();
	}

	/**
	 * Schedule to leave the voice channel in 1 minute
	 */
	public void scheduleLeave() {
		LogUtils.info("{Guild ID: %d} Scheduling leave.", guildId.asLong());
		Mono.delay(Duration.ofMinutes(1))
				.filter(ignored -> this.isLeavingScheduled())
				.doOnNext(ignored -> this.leaveVoiceChannel())
				.doOnSubscribe(ignored -> this.isLeavingScheduled.set(true))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	public void cancelLeave() {
		LogUtils.info("{Guild ID: %d} Cancelling leave.", guildId.asLong());
		this.isLeavingScheduled.set(false);
	}

	public Mono<Void> end() {
		LogUtils.info("{Guild ID: %d} Ending guild music.", guildId.asLong());
		final StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!Shadbot.getPremium().isGuildPremium(this.guildId)) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, "
					+ "it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		this.leaveVoiceChannel();
		return this.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.then();
	}

	public DiscordClient getClient() {
		return this.client;
	}

	public TrackScheduler getTrackScheduler() {
		return this.trackScheduler;
	}

	public Snowflake getMessageChannelId() {
		return this.messageChannelId;
	}

	public Mono<MessageChannel> getMessageChannel() {
		return this.client.getChannelById(this.messageChannelId)
				.cast(MessageChannel.class)
				.onErrorResume(ExceptionUtils::isKnownDiscordError, err -> Mono.empty());
	}

	public Snowflake getDjId() {
		return this.djId;
	}

	public boolean isWaitingForChoice() {
		return this.isWaitingForChoice.get();
	}

	public boolean isLeavingScheduled() {
		return this.isLeavingScheduled.get();
	}

	public void setMessageChannel(Snowflake messageChannelId) {
		this.messageChannelId = messageChannelId;
	}

	public void setDj(Snowflake djId) {
		this.djId = djId;
	}

	public void setWaitingForChoice(boolean isWaitingForChoice) {
		this.isWaitingForChoice.set(isWaitingForChoice);
	}

	public void addAudioLoadResultListener(AudioLoadResultListener listener, String identifier) {
		LogUtils.info("{Guild ID: %d} Adding audio load result listener.", guildId.asLong());
		this.listeners.put(listener, GuildMusicManager.loadItemOrdered(this.guildId, identifier, listener));
	}

	public void removeAudioLoadResultListener(AudioLoadResultListener listener) {
		LogUtils.info("{Guild ID: %d} Removing audio load result listener.", guildId.asLong());
		this.listeners.remove(listener);
		// If there is no music playing and nothing is loading, leave the voice channel
		if(this.getTrackScheduler().isStopped() && this.listeners.values().stream().allMatch(Future::isDone)) {
			this.leaveVoiceChannel();
		}
	}

	private void destroy() {
		LogUtils.info("{Guild ID: %d} Destroying guild music.", guildId.asLong());
		this.cancelLeave();
		GuildMusicManager.remove(this.guildId);
		this.listeners.values().forEach(task -> task.cancel(true));
		this.listeners.clear();
		this.trackScheduler.destroy();
	}

}
