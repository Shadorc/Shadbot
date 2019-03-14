package me.shadorc.shadbot.music;

import java.time.Duration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.VoiceConnection;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.listener.music.AudioLoadResultListener;
import me.shadorc.shadbot.listener.music.TrackEventListener;
import me.shadorc.shadbot.object.Emoji;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final Snowflake voiceChannelId;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;
	private final AtomicBoolean isWaitingForChoice;
	private final Map<AudioLoadResultListener, Future<Void>> listeners;

	private volatile VoiceConnection voiceConnection;
	private volatile Disposable leaveTask;
	private volatile Snowflake messageChannelId;
	private volatile Snowflake djId;

	public GuildMusic(DiscordClient client, Snowflake guildId, Snowflake voiceChannelId, AudioPlayer audioPlayer) {
		this.client = client;
		this.guildId = guildId;
		this.voiceChannelId = voiceChannelId;
		audioPlayer.addListener(new TrackEventListener(guildId));
		this.audioProvider = new LavaplayerAudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Shadbot.getDatabase().getDBGuild(guildId).getDefaultVol());
		this.isWaitingForChoice = new AtomicBoolean(false);
		this.listeners = new ConcurrentHashMap<>();
	}

	/**
	 * Join the voice channel only if the bot is not already in a voice channel
	 */
	public void joinVoiceChannel() {
		this.client.getChannelById(voiceChannelId)
				.cast(VoiceChannel.class)
				.filter(ignored -> this.voiceConnection == null)
				.flatMap(voiceChannel -> voiceChannel.join(spec -> spec.setProvider(this.audioProvider)))
				.doOnNext(voiceConnection -> {
					this.voiceConnection = voiceConnection;
					LogUtils.info("{Guild ID: %d} Voice channel joined.", this.getGuildId().asLong());
				})
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	/**
	 * Leave the voice channel if the bot is still in and destroy this {@link GuildMusic}
	 */
	public void leaveVoiceChannel() {
		if(this.voiceConnection != null) {
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
		this.leaveTask = Mono.delay(Duration.ofMinutes(1))
				.doOnNext(ignored -> this.leaveVoiceChannel())
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.client, err));
	}

	public void cancelLeave() {
		if(this.isLeavingScheduled()) {
			this.leaveTask.dispose();
		}
	}

	public Mono<Void> end() {
		final StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!Shadbot.getPremium().isGuildPremium(this.guildId)) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, "
					+ "it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		return Mono.fromRunnable(this::leaveVoiceChannel)
				.then(this.getMessageChannel())
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.then();
	}

	public DiscordClient getClient() {
		return this.client;
	}

	public Snowflake getGuildId() {
		return this.guildId;
	}

	public AudioProvider getAudioProvider() {
		return this.audioProvider;
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
		return this.leaveTask != null && !this.leaveTask.isDisposed();
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
		this.listeners.put(listener, GuildMusicManager.getAudioPlayerManager()
				.loadItemOrdered(this.guildId, identifier, listener));
	}

	public void removeAudioLoadResultListener(AudioLoadResultListener listener) {
		this.listeners.remove(listener);
	}

	public void destroy() {
		for(final Entry<AudioLoadResultListener, Future<Void>> entry : this.listeners.entrySet()) {
			entry.getValue().cancel(true);
			entry.getKey().terminate();
		}
		this.listeners.clear();
		this.cancelLeave();
		GuildMusicManager.remove(this.guildId);
		this.trackScheduler.destroy();
	}

}
