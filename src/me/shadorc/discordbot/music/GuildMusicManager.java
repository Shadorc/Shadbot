package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;

import me.shadorc.discordbot.Main;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager {

	public final static AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();
	private final static Map<Long, GuildMusicManager> MUSIC_MANAGERS = new HashMap<>();

	private IGuild guild;
	private IChannel channel;
	private AudioPlayer player;
	private TrackScheduler scheduler;
	private Timer leaveTimer;

	public GuildMusicManager(IGuild guild, AudioPlayerManager manager) {
		this.guild = guild;
		this.player = manager.createPlayer();
		this.scheduler = new TrackScheduler(player);
		this.player.addListener(scheduler);
		this.leaveTimer = new Timer(2*60*1000, e -> {
			this.leave();
		});
	}

	public void scheduleLeave() {
		leaveTimer.start();
	}

	public void cancelLeave() {
		leaveTimer.stop();
	}

	public void leave() {
		Main.getClient().getOurUser().getVoiceStateForGuild(guild).getChannel().leave();
		scheduler.stop();
		leaveTimer.stop();
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

	public IChannel getRequestedChannel() {
		return channel;
	}

	public void setRequestedChannel(IChannel channel) {
		this.channel = channel;
	}

	public boolean isCancelling() {
		return leaveTimer.isRunning();
	}

	public static synchronized GuildMusicManager getGuildAudioPlayer(IGuild guild) {
		GuildMusicManager musicManager = MUSIC_MANAGERS.get(guild.getLongID());

		if(musicManager == null) {
			musicManager = new GuildMusicManager(guild, PLAYER_MANAGER);
			MUSIC_MANAGERS.put(guild.getLongID(), musicManager);
		}

		guild.getAudioManager().setAudioProvider(musicManager.getAudioProvider());

		return musicManager;
	}
}
