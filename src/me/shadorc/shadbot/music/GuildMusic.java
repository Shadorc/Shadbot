package me.shadorc.shadbot.music;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
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
	private final AtomicBoolean isWaitingForChoice;
	private final AtomicBoolean isLeavingScheduled;

	private volatile Snowflake messageChannelId;
	private volatile Snowflake djId;

	public GuildMusic(DiscordClient client, Snowflake guildId, TrackScheduler trackScheduler) {
		this.client = client;
		this.guildId = guildId;
		this.trackScheduler = trackScheduler;
		this.listeners = new ConcurrentHashMap<>();
		this.isWaitingForChoice = new AtomicBoolean(false);
		this.isLeavingScheduled = new AtomicBoolean(false);
	}

	/**
	 * Schedule to leave the voice channel in 1 minute
	 */
	public void scheduleLeave() {
		LogUtils.debug("{Guild ID: %d} Scheduling auto-leave.", this.guildId.asLong());
		Mono.delay(Duration.ofMinutes(1))
				.filter(ignored -> this.isLeavingScheduled())
				.doOnNext(ignored -> MusicManager.getConnection(this.guildId).leaveVoiceChannel())
				.doOnSubscribe(ignored -> this.isLeavingScheduled.set(true))
				.doOnTerminate(() -> this.isLeavingScheduled.set(false))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	public void cancelLeave() {
		LogUtils.debug("{Guild ID: %d} Cancelling auto-leave.", this.guildId.asLong());
		this.isLeavingScheduled.set(false);
	}

	public Mono<Void> end() {
		LogUtils.debug("{Guild ID: %d} Ending guild music.", this.guildId.asLong());
		final StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!Shadbot.getPremium().isGuildPremium(this.guildId)) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, "
					+ "it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}

		MusicManager.getConnection(this.guildId).leaveVoiceChannel();
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
		LogUtils.debug("{Guild ID: %d} Adding audio load result listener.", this.guildId.asLong());
		this.listeners.put(listener, MusicManager.loadItemOrdered(this.guildId, identifier, listener));
	}

	public void removeAudioLoadResultListener(AudioLoadResultListener listener) {
		LogUtils.debug("{Guild ID: %d} Removing audio load result listener.", this.guildId.asLong());
		this.listeners.remove(listener);
		// If there is no music playing and nothing is loading, leave the voice channel
		if(this.getTrackScheduler().isStopped() && this.listeners.values().stream().allMatch(Future::isDone)) {
			MusicManager.getConnection(this.guildId).leaveVoiceChannel();
		}
	}

	protected void destroy() {
		this.cancelLeave();
		this.listeners.values().forEach(task -> task.cancel(true));
		this.listeners.clear();
		this.trackScheduler.destroy();
	}

}
