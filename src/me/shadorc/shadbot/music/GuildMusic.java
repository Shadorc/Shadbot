package me.shadorc.shadbot.music;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class GuildMusic {

	private final Snowflake guildId;
	private final AudioPlayer audioPlayer;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;

	private ScheduledFuture<?> leaveTask;
	private Snowflake channelId;
	private Snowflake userDjId;
	private boolean isWaiting;

	public GuildMusic(Snowflake guildId, AudioPlayerManager audioPlayerManager) {
		this.guildId = guildId;
		this.audioPlayer = audioPlayerManager.createPlayer();
		this.audioProvider = new AudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guildId).getDefaultVol());
		this.audioPlayer.addListener(new AudioEventListener(this));
	}

	public void scheduleLeave() {
		leaveTask = Shadbot.getScheduler().schedule(() -> this.leaveVoiceChannel(), 1, TimeUnit.MINUTES);
	}

	public void cancelLeave() {
		if(leaveTask != null) {
			leaveTask.cancel(false);
		}
	}

	public void joinVoiceChannel(VoiceChannel voiceChannel) {
		if(voiceChannel.getClient().getSelf().getVoiceStateForGuild(guild).getChannel() == null) {
			voiceChannel.join();
			LogUtils.infof("{Guild ID: %d} Voice channel joined.", voiceChannel.getGuild().getLongID());
		}
	}

	public void end() {
		StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!PremiumManager.isPremium(channel.getGuild())) {
			strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		BotUtils.sendMessage(strBuilder.toString(), channel);
		this.leaveVoiceChannel();
	}

	public void leaveVoiceChannel() {
		// Leaving a voice channel can take up to 30 seconds to be executed
		// We execute it in a separate thread pool to avoid thread blocking
		ShardManager.execute(channel.getGuild(), () -> {
			IVoiceChannel voiceChannel = Shadbot.getClient().getSelf().getVoiceStateForGuild(guild).getChannel();
			if(voiceChannel != null && voiceChannel.getShard().isReady()) {
				RequestBuffer.request(() -> {
					voiceChannel.leave();
				});
			}
		});
	}

	public void delete() {
		this.cancelLeave();
		GuildMusicManager.GUILD_MUSIC_MAP.remove(guild.getLongID());
		audioPlayer.destroy();
		trackScheduler.clearPlaylist();
	}

	public IChannel getChannel() {
		return channel;
	}

	public IUser getDj() {
		return userDj;
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

	public void setChannel(IChannel channel) {
		this.channel = channel;
	}

	public void setDj(IUser userDj) {
		this.userDj = userDj;
	}

	public void setWaiting(boolean isWaiting) {
		this.isWaiting = isWaiting;
	}

}
