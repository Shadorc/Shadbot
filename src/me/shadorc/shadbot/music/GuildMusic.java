package me.shadorc.shadbot.music;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.RequestBuffer;

public class GuildMusic {

	private final IGuild guild;
	private final AudioPlayer audioPlayer;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;

	private ScheduledFuture<?> leaveTask;
	private IChannel channel;
	private IUser userDj;
	private boolean isWaiting;

	public GuildMusic(IGuild guild, AudioPlayerManager audioPlayerManager) {
		this.guild = guild;
		this.audioPlayer = audioPlayerManager.createPlayer();
		this.audioProvider = new AudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guild).getDefaultVol());
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

	public void joinVoiceChannel(IVoiceChannel voiceChannel) {
		if(voiceChannel.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel() == null) {
			voiceChannel.join();
			LogUtils.infof("{Guild ID: %d} Voice channel joined.", voiceChannel.getGuild().getLongID());
		}
	}

	public void end() {
		Shadbot.getEventThreadPool().submit(() -> {
			StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
			if(!PremiumManager.isPremium(channel.getGuild())) {
				strBuilder.append(String.format(" If you like me, you can make a donation on **%s**, it will help my creator keeping me alive :heart:",
						Config.PATREON_URL));
			}
			BotUtils.sendMessage(strBuilder.toString(), channel);
			this.leaveVoiceChannel();
		});
	}

	public void leaveVoiceChannel() {
		IVoiceChannel voiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(voiceChannel != null && voiceChannel.getShard().isReady()) {
			RequestBuffer.request(() -> {
				voiceChannel.leave();
			}).get();
		}
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
