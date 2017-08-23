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
		BotUtils.sendMessage(Emoji.INFO + " End of the playlist.", channel);
		this.leaveVoiceChannel();
	}

	public boolean joinVoiceChannel(IVoiceChannel voiceChannel) {
		if(!BotUtils.hasPermission(voiceChannel, Permissions.VOICE_CONNECT, Permissions.VOICE_SPEAK)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect/speak in this voice channel due to the lack of permission."
					+ "\nPlease, check my permissions and channel-specific ones to verify that **Voice connect** and **Voice speak** "
					+ "are checked.", channel);
			LogUtils.info("{Guild: " + voiceChannel.getGuild().getName() + " (ID: " + voiceChannel.getGuild().getStringID() + ")} "
					+ "Shadbot wasn't allowed to connect/speak in a voice channel.");
			return false;
		}
		LogUtils.info("{GuildMusicManager} {Guild: " + voiceChannel.getGuild().getName()
				+ " (ID: " + voiceChannel.getGuild().getStringID() + ")} Voice channel joined.");
		voiceChannel.join();
		return true;
	}

	public void leaveVoiceChannel() {
		// FIXME: Temporary fix to avoid SocketClosed exception
		new Thread(new Runnable() {
			@Override
			@SuppressWarnings("PMD.AccessorMethodGeneration")
			public void run() {
				IVoiceChannel voiceChannel = Shadbot.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel();
				if(voiceChannel != null) {
					voiceChannel.leave();
				}
				GuildMusicManager.this.cancelLeave();
				audioPlayer.destroy();
				MUSIC_MANAGERS.remove(guild);
				LogUtils.info("{GuildMusicManager} {Guild: " + guild.getName()
						+ " (ID: " + guild.getStringID() + ")} Voice channel leaved.");
			}
		}).start();
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
