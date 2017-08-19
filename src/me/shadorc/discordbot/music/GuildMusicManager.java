package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

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

	private final static Map<IGuild, GuildMusicManager> MUSIC_MANAGERS = new HashMap<>();

	private final IGuild guild;
	private final AudioPlayer player;
	private final TrackScheduler scheduler;
	private final AudioEventListener audioEventListener;
	private final Timer leaveTimer;

	private IChannel channel;
	private IUser userDj;

	// TODO: Remove
	private boolean ended;

	private GuildMusicManager(IGuild guild, AudioPlayerManager manager) {
		this.guild = guild;
		this.player = manager.createPlayer();
		this.scheduler = new TrackScheduler(guild, player);
		this.audioEventListener = new AudioEventListener(guild, scheduler);
		this.player.addListener(audioEventListener);
		this.leaveTimer = new Timer(60 * 1000, event -> {
			this.scheduler.stop();
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
			LogUtils.warn("Debug: Music has tried to end multiple times...");
			return;
		}
		ended = true;
		BotUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel);
		this.leaveVoiceChannel();
	}

	public boolean joinVoiceChannel(IVoiceChannel voiceChannel) {
		if(!BotUtils.hasPermission(voiceChannel, Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK)) {
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
		return new AudioProvider(player);
	}

	public AudioPlayer getAudioPlayer() {
		return player;
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

	public static synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
		GuildMusicManager musicManager = MUSIC_MANAGERS.get(guild);

		if(musicManager == null) {
			musicManager = new GuildMusicManager(guild, PLAYER_MANAGER);
			MUSIC_MANAGERS.put(guild, musicManager);
		}

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}
}
