package me.shadorc.discordbot.events;

import java.util.List;

import javax.swing.Timer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageListener {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private final String identifier;
	private final IVoiceChannel botVoiceChannel;
	private final IVoiceChannel userVoiceChannel;
	private final GuildMusicManager musicManager;

	private List<AudioTrack> resultsTracks;
	private Timer cancelTimer;

	public AudioLoadResultListener(String identifier, IVoiceChannel botVoiceChannel, IVoiceChannel userVoiceChannel, GuildMusicManager musicManager) {
		this.identifier = identifier;
		this.botVoiceChannel = botVoiceChannel;
		this.userVoiceChannel = userVoiceChannel;
		this.musicManager = musicManager;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", musicManager.getChannel());
			return;
		}

		if(musicManager.getScheduler().isPlaying()) {
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(track.getInfo()) + "** has been added to the playlist.", musicManager.getChannel());
		}
		musicManager.getScheduler().queue(track);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		// SoundCloud send empty playlist when no result are found
		if(playlist.getTracks().isEmpty()) {
			BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + identifier.replaceAll(YT_SEARCH + "|" + SC_SEARCH, "") + "\"", musicManager.getChannel());
			return;
		}

		List<AudioTrack> tracks = playlist.getTracks();

		if(identifier.startsWith(YT_SEARCH) || identifier.startsWith(SC_SEARCH)) {
			StringBuilder strBuilder = new StringBuilder();
			for(int i = 0; i < Math.min(5, tracks.size()); i++) {
				strBuilder.append("\n\t**" + (i + 1) + ".** " + StringUtils.formatTrackName(tracks.get(i).getInfo()));
			}

			EmbedBuilder embed = new EmbedBuilder()
					.withAuthorName("Results (Type: " + Storage.getSetting(musicManager.getChannel().getGuild(), Setting.PREFIX) + "cancel to cancel)")
					.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
					.withDescription("**Enter your choice.**\n" + strBuilder.toString())
					.withFooterText("This choice will be canceled in 15 seconds.");
			BotUtils.sendEmbed(embed.build(), musicManager.getChannel());

			cancelTimer = new Timer(15 * 1000, event -> {
				this.stopWaiting();
			});
			cancelTimer.start();

			resultsTracks = tracks;
			MessageManager.addListener(musicManager.getChannel(), this);
			return;
		}

		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", musicManager.getChannel());
			return;
		}

		for(int i = 0; i < Math.min(200, tracks.size()); i++) {
			AudioTrack track = tracks.get(i);
			musicManager.getScheduler().queue(track);
		}
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " " + musicManager.getScheduler().getPlaylist().size() + " musics have been added to the playlist.", musicManager.getChannel());
	}

	@Override
	public void noMatches() {
		BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + identifier.replaceAll(YT_SEARCH + "|" + SC_SEARCH, "") + "\"", musicManager.getChannel());

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public void loadFailed(FriendlyException err) {
		if(err.severity.equals(FriendlyException.Severity.FAULT)) {
			LogUtils.warn("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} "
					+ "Load failed (Severity: " + err.severity.toString() + "), Shadbot might be able to continue playing: " + err.getMessage());
		} else {
			BotUtils.sendMessage(Emoji.GEAR + " Sorry, " + err.getMessage().toLowerCase(), musicManager.getChannel());
			LogUtils.warn("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} Load failed: " + err.getMessage());
		}

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public void onMessageReceived(IMessage message) {
		if(!message.getAuthor().equals(musicManager.getDj())) {
			return;
		}

		String prefix = Storage.getSetting(musicManager.getChannel().getGuild(), Setting.PREFIX).toString();
		if(message.getContent().equalsIgnoreCase(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Choice canceled.", musicManager.getChannel());
			this.stopWaiting();
			return;
		}

		String numStr = message.getContent().replace(prefix, "");
		if(!StringUtils.isInteger(numStr)) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " This is not a valid choice. "
					+ "You can use \"" + prefix + "cancel\" to cancel.",
					musicManager.getChannel());
			LogUtils.info("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} Invalid choice: " + message.getContent());
			return;
		}

		int num = Integer.parseInt(numStr);
		if(num < 1 || num > Math.min(5, resultsTracks.size())) {
			BotUtils.sendMessage(Emoji.EXCLAMATION + " This is not a valid choice. "
					+ "You can use \"" + prefix + "cancel\" to cancel.",
					musicManager.getChannel());
			LogUtils.info("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} Invalid choice: " + message.getContent());
			return;
		}

		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			BotUtils.sendMessage(Emoji.ACCESS_DENIED + " I cannot connect to this voice channel due to the lack of permission.", musicManager.getChannel());
			return;
		}

		AudioTrack track = resultsTracks.get(num - 1);
		if(musicManager.getScheduler().isPlaying()) {
			BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(track.getInfo()) + "** has been added to the playlist.", musicManager.getChannel());
		}
		musicManager.getScheduler().queue(track);

		this.stopWaiting();
	}

	private void stopWaiting() {
		cancelTimer.stop();
		resultsTracks.clear();
		MessageManager.removeListener(musicManager.getChannel());
	}
}
