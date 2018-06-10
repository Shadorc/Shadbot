package me.shadorc.shadbot.music;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;

	private ScheduledFuture<?> leaveTask;
	private Snowflake voiceChannelId;
	private Snowflake messageChannelId;
	private Snowflake djId;
	private boolean isWaiting;

	public GuildMusic(DiscordClient client, Snowflake guildId, AudioPlayerManager audioPlayerManager) {
		this.client = client;
		this.guildId = guildId;

		AudioPlayer audioPlayer = audioPlayerManager.createPlayer();
		audioPlayer.addListener(new AudioEventListener(this));
		this.audioProvider = new MusicProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guildId).getDefaultVol());
	}

	public void scheduleLeave() {
		leaveTask = GuildMusicManager.VOICE_LEAVE_SCHEDULER
				.schedule(this::leaveVoiceChannel, 1, TimeUnit.MINUTES);
	}

	public void cancelLeave() {
		if(leaveTask != null) {
			leaveTask.cancel(false);
		}
	}

	/**
	 * Join a voice channel only if the bot is not already in a voice channel
	 * 
	 * @param voiceChannelId - the voice channel ID to join
	 */
	public void joinVoiceChannel(Snowflake voiceChannelId) {
		client.getMemberById(guildId, client.getSelfId().get())
				.flatMap(Member::getVoiceState)
				.filter(voiceState -> !voiceState.getChannelId().isPresent())
				.flatMap(voiceState -> client.getVoiceChannelById(voiceChannelId))
				.subscribe(voiceChannel -> {
					this.voiceChannelId = voiceChannelId;
					voiceChannel.join();
				});
	}

	public void leaveVoiceChannel() {
		client.getVoiceChannelById(voiceChannelId).subscribe(voiceChannel -> {
			// TODO: Leave voice channel
		});
	}

	public void end() {
		StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!PremiumManager.isGuildPremium(guildId)) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, "
					+ "it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		BotUtils.sendMessage(strBuilder.toString(), client.getMessageChannelById(messageChannelId));
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

	public void setChannel(Snowflake channelId) {
		this.messageChannelId = channelId;
	}

	public void setDj(Snowflake djId) {
		this.djId = djId;
	}

	public void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}

}
