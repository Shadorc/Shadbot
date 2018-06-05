package me.shadorc.shadbot.music;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.publisher.Mono;

public class GuildMusic {

	private final DiscordClient client;
	private final Snowflake guildId;
	private final AudioPlayer audioPlayer;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;

	private ScheduledFuture<?> leaveTask;
	private Snowflake voiceChannelId;
	private Snowflake channelId;
	private Snowflake djId;
	private boolean isWaiting;

	public GuildMusic(DiscordClient client, Snowflake guildId, AudioPlayerManager audioPlayerManager) {
		this.client = client;
		this.guildId = guildId;
		this.audioPlayer = audioPlayerManager.createPlayer();
		this.audioProvider = new IshAudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guildId).getDefaultVol());
		this.audioPlayer.addListener(new AudioEventListener(this));
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

	public void joinVoiceChannel(Snowflake voiceChannelId) {
		client.getSelf()
				.map(self -> self.asMember(guildId))
				.subscribe(selfMember -> {
					// TODO: Check if voice state is null
					// selfMember.getVoiceState();
					client.getVoiceChannelById(voiceChannelId).subscribe(voiceChannel -> {
						this.voiceChannelId = voiceChannelId;
						// TODO: audioReceiver
						voiceChannel.join(this.getAudioProvider(), null).subscribe();
						LogUtils.infof("{Guild ID: %s} Voice channel joined.", guildId);
					});
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
		BotUtils.sendMessage(strBuilder.toString(), client.getMessageChannelById(channelId));
		this.leaveVoiceChannel();
	}

	public void delete() {
		this.cancelLeave();
		GuildMusicManager.GUILD_MUSIC_MAP.remove(guildId);
		audioPlayer.destroy();
		trackScheduler.clearPlaylist();
	}

	public DiscordClient getClient() {
		return client;
	}

	public Snowflake getGuildId() {
		return guildId;
	}

	public Snowflake getChannelId() {
		return channelId;
	}

	public Mono<MessageChannel> getChannel() {
		return client.getMessageChannelById(channelId);
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
		this.channelId = channelId;
	}

	public void setDj(Snowflake djId) {
		this.djId = djId;
	}

	public void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}

}
