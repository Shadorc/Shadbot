package me.shadorc.discordbot.music;

import java.util.HashMap;
import java.util.Map;

import javax.swing.Timer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.shadorc.discordbot.Main;
import me.shadorc.discordbot.utility.BotUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class GuildMusicManager extends AudioEventAdapter {

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
		this.player.addListener(this);
		this.leaveTimer = new Timer(2*60*1000, e -> {
			this.scheduler.stop();
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

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		//Create a new Thread avoid java.net.SocketException by leaving the time to the sockets to close
		new Thread(() -> {
			if(endReason.mayStartNext) {
				if(scheduler.isRepeating()) {
					scheduler.queue(track.makeClone());
				} else if(!scheduler.nextTrack()) {
					BotUtils.sendMessage(":grey_exclamation: Fin de la playlist.", channel);
					GuildMusicManager.getGuildAudioPlayer(guild).leave();
				}
			}
		}).start();
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
