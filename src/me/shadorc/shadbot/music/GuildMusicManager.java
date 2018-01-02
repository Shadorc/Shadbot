package me.shadorc.shadbot.music;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.music.AudioEventListener;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.ThreadPoolUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class GuildMusicManager {

	public final static AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();

	private final static ConcurrentHashMap<Long, GuildMusicManager> MUSIC_MANAGERS = new ConcurrentHashMap<>();
	private final static ScheduledThreadPoolExecutor EXECUTOR = ThreadPoolUtils.newSingleScheduledThreadPoolExecutor("Shadbot-MusicLeaver-%d");

	private final IGuild guild;
	private final AudioPlayer audioPlayer;
	private final AudioProvider audioProvider;
	private final TrackScheduler trackScheduler;

	private ScheduledFuture<?> leaveTask;
	private IChannel channel;
	private IUser userDj;
	private boolean isWaiting;

	private GuildMusicManager(IGuild guild, AudioPlayerManager audioPlayerManager) {
		this.guild = guild;
		this.audioPlayer = audioPlayerManager.createPlayer();
		this.audioProvider = new AudioProvider(audioPlayer);
		this.trackScheduler = new TrackScheduler(audioPlayer, Database.getDBGuild(guild).getDefaultVol());
		this.audioPlayer.addListener(new AudioEventListener(this));
	}

	public void scheduleLeave() {
		leaveTask = EXECUTOR.schedule(() -> this.leaveVoiceChannel(), 1, TimeUnit.MINUTES);
	}

	public void cancelLeave() {
		if(leaveTask != null) {
			leaveTask.cancel(false);
		}
	}

	// TODO: When do 'force' is used ?
	// FIXME: Missing permissions sometimes thrown when joining
	public void joinVoiceChannel(IVoiceChannel voiceChannel, boolean forceJoin) {
		if(voiceChannel.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel() == null || forceJoin) {
			voiceChannel.join();
			LogUtils.infof("{Guild ID: %d} Voice channel joined.", voiceChannel.getGuild().getLongID());
		}
	}

	public void end() {
		StringBuilder strBuilder = new StringBuilder(Emoji.INFO + " End of the playlist.");
		if(!PremiumManager.isGuildPremium(channel.getGuild())) {
			strBuilder.append(String.format(" If you like me, you can make a donation on %s, it will help my creator keeping me alive :heart:",
					Config.PATREON_URL));
		}
		BotUtils.sendMessage(strBuilder.toString(), channel);
		this.leaveVoiceChannel();
	}

	public void leaveVoiceChannel() {
		IVoiceChannel voiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(voiceChannel != null && voiceChannel.getShard().isReady()) {
			voiceChannel.leave();
		}
	}

	public void delete() {
		this.cancelLeave();
		MUSIC_MANAGERS.remove(guild.getLongID());
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

	public static GuildMusicManager createGuildMusicManager(IGuild guild) {
		GuildMusicManager musicManager = new GuildMusicManager(guild, PLAYER_MANAGER);
		MUSIC_MANAGERS.put(guild.getLongID(), musicManager);

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}

	public static GuildMusicManager getGuildMusicManager(IGuild guild) {
		return MUSIC_MANAGERS.get(guild.getLongID());
	}

	public static void putGuildMusicManagerIfAbsent(IGuild guild, GuildMusicManager musicManager) {
		MUSIC_MANAGERS.putIfAbsent(guild.getLongID(), musicManager);
	}
}
