package me.shadorc.discordbot.music;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.data.Storage;
import me.shadorc.discordbot.data.Storage.Setting;
import me.shadorc.discordbot.events.music.AudioEventListener;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class GuildMusicManager {

	public final static AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();

	private final static ConcurrentHashMap<IGuild, GuildMusicManager> MUSIC_MANAGERS = new ConcurrentHashMap<>();

	private final IGuild guild;
	private final AudioPlayer audioPlayer;
	private final AudioProvider audioProvider;
	private final TrackScheduler scheduler;
	private final Timer leaveTimer;

	private IChannel channel;
	private IUser userDj;

	private GuildMusicManager(IGuild guild, AudioPlayerManager manager) {
		this.guild = guild;
		this.audioPlayer = manager.createPlayer();
		this.audioProvider = new AudioProvider(audioPlayer);
		this.scheduler = new TrackScheduler(audioPlayer, Integer.parseInt(Storage.getSetting(guild, Setting.DEFAULT_VOLUME).toString()));
		this.audioPlayer.addListener(new AudioEventListener(this));
		this.leaveTimer = new Timer((int) TimeUnit.MINUTES.toMillis(1), event -> {
			this.leaveVoiceChannel();
		});
	}

	public void scheduleLeave() {
		leaveTimer.start();
	}

	public void cancelLeave() {
		leaveTimer.stop();
	}

	public void joinVoiceChannel(IVoiceChannel voiceChannel, boolean force) {
		if(Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel() == null || force) {
			voiceChannel.join();
			LogUtils.info("{Guild ID: " + voiceChannel.getGuild().getLongID() + "} Voice channel joined.");
		}
	}

	public void end() {
		// Do not block the lavaplayer thread to allow the socket to be closed in time, avoiding a SocketClosed exception
		new Thread(() -> {
			BotUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel);
			this.leaveVoiceChannel();
		}).start();
	}

	public void leaveVoiceChannel() {
		IVoiceChannel voiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(voiceChannel != null) {
			voiceChannel.leave();
			LogUtils.info("{Guild ID: " + guild.getLongID() + ")} Voice channel leaved.");
		}
		this.cancelLeave();
		audioPlayer.destroy();
		MUSIC_MANAGERS.remove(guild);
	}

	public void setChannel(IChannel channel) {
		this.channel = channel;
	}

	public void setDj(IUser userDj) {
		this.userDj = userDj;
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
		return scheduler;
	}

	public boolean isLeavingScheduled() {
		return leaveTimer.isRunning();
	}

	public static GuildMusicManager createGuildMusicManager(IGuild guild) {
		GuildMusicManager musicManager = new GuildMusicManager(guild, PLAYER_MANAGER);
		MUSIC_MANAGERS.put(guild, musicManager);

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}

	public static void putGuildMusicManagerIfAbsent(IGuild guild, GuildMusicManager musicManager) {
		MUSIC_MANAGERS.putIfAbsent(guild, musicManager);
	}

	public static GuildMusicManager getGuildMusicManager(IGuild guild) {
		return MUSIC_MANAGERS.get(guild);
	}
}
