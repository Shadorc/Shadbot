package me.shadorc.shadbot.music;

import java.time.Duration;
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
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.exception.ExceptionUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;
	private final AtomicBoolean isWaitingForChoice;

	private volatile VoiceConnection voiceConnection;
	private Disposable leaveTask;
	private Snowflake messageChannelId;
	private Snowflake djId;

	public GuildMusic(DiscordClient client, Snowflake guildId, AudioPlayer audioPlayer) {
		this.client = client;
		this.guildId = guildId;
		audioPlayer.addListener(new AudioEventListener(this));
		this.audioProvider = new LavaplayerAudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Shadbot.getDatabase().getDBGuild(guildId).getDefaultVol());
		this.isWaitingForChoice = new AtomicBoolean(false);
	}

	/**
	 * Join a voice channel only if the bot is not already in a voice channel
	 *
	 * @param voiceChannelId - the voice channel ID to join
	 */
	public void joinVoiceChannel(Snowflake voiceChannelId) {
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

	public void leaveVoiceChannel() {
		if(this.voiceConnection != null) {
			this.voiceConnection.disconnect();
			this.voiceConnection = null;
		}
	}

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

	public void destroy() {
		this.cancelLeave();
		GuildMusicManager.removeGuildMusic(this.guildId);
		this.trackScheduler.destroy();
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

}
