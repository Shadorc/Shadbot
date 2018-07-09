package me.shadorc.shadbot.music;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import discord4j.core.DiscordClient;
import discord4j.core.VoiceConnectionController;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import discord4j.voice.AudioReceiver;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.SchedulerUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final AtomicBoolean isInVoiceChannel;
	private final AudioProvider audioProvider;
	private final AudioReceiver audioReceiver;
	private final TrackScheduler trackScheduler;

	private VoiceConnectionController controller;
	private ScheduledFuture<?> leaveTask;
	private Snowflake messageChannelId;
	private Snowflake djId;
	private boolean isWaiting;
	private boolean isDone;

	public GuildMusic(DiscordClient client, Snowflake guildId, AudioPlayerManager audioPlayerManager) {
		this.client = client;
		this.guildId = guildId;
		this.isInVoiceChannel = new AtomicBoolean(false);

		AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
		audioPlayer.addListener(new AudioEventListener(this));
		this.audioProvider = new MusicProvider(audioPlayer, this);
		this.audioReceiver = new MusicReceiver();
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guildId).getDefaultVol());
	}

	public void scheduleLeave() {
		leaveTask = SchedulerUtils.schedule(this::leaveVoiceChannel, 1, TimeUnit.MINUTES);
	}

	public void cancelLeave() {
		if(this.isLeavingScheduled()) {
			leaveTask.cancel(false);
		}
	}

	/**
	 * Join a voice channel only if the bot is not already in a voice channel
	 * 
	 * @param voiceChannelId - the voice channel ID to join
	 */
	public void joinVoiceChannel(Snowflake voiceChannelId) {
		if(!isInVoiceChannel.get()) {
			client.getVoiceChannelById(voiceChannelId)
					.flatMap(VoiceChannel::join)
					.map(controller -> this.controller = controller)
					.flatMap(controller -> {
						isInVoiceChannel.set(true);
						return controller.connect(audioProvider, audioReceiver);
					})
					.doOnError(err -> {
						BotUtils.sendMessage(Emoji.RED_FLAG + "Sorry, something went wrong during the connection to the voice channel... "
								+ "My developer has been warned.",
								this.getMessageChannel())
								.subscribe();
						LogUtils.error(client, err,
								String.format("{%d} An unknown error occurred while joining a voice channel.", guildId.asLong()));
						// FIXME: this does not work
						this.leaveVoiceChannel();
					})
					.subscribe();
		}
	}

	public void leaveVoiceChannel() {
		if(isInVoiceChannel.get()) {
			isDone = true;
			controller.disconnect();
			isInVoiceChannel.set(false);
		}
	}

	public void end() {
		StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!PremiumManager.isGuildPremium(guildId)) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, "
					+ "it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		BotUtils.sendMessage(strBuilder.toString(), client.getMessageChannelById(messageChannelId)).subscribe();
		this.leaveVoiceChannel();
	}

	public void destroy() {
		this.cancelLeave();
		GuildMusicManager.GUILD_MUSIC_MAP.remove(guildId);
		trackScheduler.destroy();
	}

	public DiscordClient getClient() {
		return client;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getMessageChannelId() {
		return messageChannelId;
	}

	public Mono<MessageChannel> getMessageChannel() {
		return client.getMessageChannelById(messageChannelId);
	}

	public Snowflake getDjId() {
		return djId;
	}

	public AudioProvider getAudioProvider() {
		return audioProvider;
	}

	public TrackScheduler getScheduler() {
		return trackScheduler;
	}

	public boolean isLeavingScheduled() {
		return leaveTask != null && !leaveTask.isDone();
	}

	public boolean isWaiting() {
		return isWaiting;
	}

	public boolean isDone() {
		return isDone;
	}

	public void setMessageChannel(Snowflake messageChannelId) {
		this.messageChannelId = messageChannelId;
	}

	public void setDj(Snowflake djId) {
		this.djId = djId;
	}

	public void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}

}
