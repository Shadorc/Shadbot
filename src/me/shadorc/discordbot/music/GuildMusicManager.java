package me.shadorc.discordbot.music;

import java.util.concurrent.ConcurrentHashMap;

import javax.swing.Timer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.events.AudioEventListener;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.handle.obj.Permissions;

public class GuildMusicManager {

	public final static AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();

	private final static ConcurrentHashMap<IGuild, GuildMusicManager> MUSIC_MANAGERS = new ConcurrentHashMap<>();

	private final IGuild guild;
	private final AudioPlayer audioPlayer;
	private final TrackScheduler scheduler;
	private final AudioEventListener audioEventListener;
	private final Timer leaveTimer;

	private IChannel channel;
	private IUser userDj;

	// TODO: Remove
	private boolean ended;

	private GuildMusicManager(IGuild guild, AudioPlayerManager manager) {
		this.guild = guild;
		this.audioPlayer = manager.createPlayer();
		this.scheduler = new TrackScheduler(guild, audioPlayer);
		this.audioEventListener = new AudioEventListener(guild, scheduler);
		this.audioPlayer.addListener(audioEventListener);
		this.leaveTimer = new Timer(60 * 1000, event -> {
			this.leaveVoiceChannel();
		});
	}

	public void scheduleLeave() {
		leaveTimer.start();
	}

	public void cancelLeave() {
		leaveTimer.stop();
	}

	public void end() {
		// TODO: Remove
		if(ended) {
			LogUtils.warn("{GuildMusicManager} {Guild: " + channel.getGuild().getName()
					+ " (ID: " + channel.getGuild().getStringID() + ")} Music has tried to end multiple times.");
			return;
		}
		ended = true;
		BotUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel);
		this.leaveVoiceChannel();
	}

	public boolean joinVoiceChannel(IVoiceChannel voiceChannel) {
		if(!BotUtils.hasPermission(voiceChannel, Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK)) {
			LogUtils.warn("{Guild: " + voiceChannel.getGuild().getName() + " (ID: " + voiceChannel.getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to connect/speak.");
			return false;
		}
		voiceChannel.join();
		return true;
	}

	public void leaveVoiceChannel() {
		IVoiceChannel voiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
		if(voiceChannel != null) {
			voiceChannel.leave();
		}
		leaveTimer.stop();
		audioPlayer.destroy();
		MUSIC_MANAGERS.remove(guild);
	}

	public void setChannel(IChannel channel) {
		this.channel = channel;
		this.audioEventListener.setChannel(channel);
	}

	public void setDj(IUser userDj) {
		this.userDj = userDj;
	}

	public IUser getDj() {
		return userDj;
	}

	public AudioProvider getAudioProvider() {
		return new AudioProvider(audioPlayer);
	}

	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}

	public TrackScheduler getScheduler() {
		return scheduler;
	}

	public IChannel getChannel() {
		return channel;
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

	public static GuildMusicManager getGuildMusicManager(IGuild guild) {
		return MUSIC_MANAGERS.get(guild);
	}
}
