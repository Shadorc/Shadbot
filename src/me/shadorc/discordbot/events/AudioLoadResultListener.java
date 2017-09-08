package me.shadorc.discordbot.events;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Timer;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.Setting;
import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.discordbot.music.GuildMusicManager;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.StringUtils;
import me.shadorc.discordbot.utils.Utils;
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

			if(MessageManager.isWaitingForMessage(musicManager.getChannel())) {
				BotUtils.sendMessage(Emoji.HOURGLASS + " Someone is already selecting a music, please wait for him to finish.", musicManager.getChannel());
				return;
			}

			StringBuilder strBuilder = new StringBuilder();
			for(int i = 0; i < Math.min(5, tracks.size()); i++) {
				strBuilder.append("\n\t**" + (i + 1) + ".** " + StringUtils.formatTrackName(tracks.get(i).getInfo()));
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.withAuthorName("Results (Use " + Storage.getSetting(musicManager.getChannel().getGuild(), Setting.PREFIX) + "cancel to cancel the selection)")
					.withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
					.appendDescription("**Select a music by typing the corresponding number.**"
							+ "\nYou can choose several musics by separating them with a comma."
							+ "\nExample: 1,3,4"
							+ "\n" + strBuilder.toString())
					.withFooterText("This choice will be canceled in 20 seconds.");
			BotUtils.sendEmbed(embed.build(), musicManager.getChannel());

			cancelTimer = new Timer(20 * 1000, event -> {
				this.stopWaiting();
			});
			cancelTimer.start();

			resultsTracks = tracks;
			MessageManager.addListener(musicManager.getChannel(), this);
			return;
		}

		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			return;
		}

		for(int i = 0; i < Math.min(200, tracks.size()); i++) {
			musicManager.getScheduler().queue(tracks.get(i));
		}
		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " " + musicManager.getScheduler().getPlaylist().size() + " musics have been added to the playlist.", musicManager.getChannel());
	}

	@Override
	public void noMatches() {
		BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + identifier.replaceAll(YT_SEARCH + "|" + SC_SEARCH, "") + "\"", musicManager.getChannel());
		LogUtils.info("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
				+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} "
				+ "No matches: " + identifier);

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public void loadFailed(FriendlyException err) {
		String errMessage = Jsoup.parse(err.getMessage().replace("Watch on YouTube", "")).text().trim();
		if(err.severity.equals(FriendlyException.Severity.FAULT)) {
			LogUtils.warn("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} "
					+ "Load failed, Shadbot might be able to continue playing: " + errMessage);
		} else {
			BotUtils.sendMessage(Emoji.GEAR + " Sorry, " + errMessage.toLowerCase(), musicManager.getChannel());
			LogUtils.info("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
					+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} Load failed: " + errMessage);
		}

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		if(!message.getAuthor().equals(musicManager.getDj())) {
			return false;
		}

		String prefix = Storage.getSetting(musicManager.getChannel().getGuild(), Setting.PREFIX).toString();
		if(message.getContent().equalsIgnoreCase(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Choice canceled.", musicManager.getChannel());
			this.stopWaiting();
			return true;
		}

		String content = message.getContent().replace("/", "").replace("play", "").trim();

		List<Integer> choices = new ArrayList<>();
		for(String str : content.split(",")) {
			// Remove all non numeric characters
			String numStr = str.replaceAll("[^\\d]", "").trim();
			if(!StringUtils.isPositiveInt(numStr)) {
				this.sendInvalidChoice(str.trim(), prefix, message);
				return true;
			}

			int num = Integer.parseInt(numStr);
			if(num < 1 || num > Math.min(5, resultsTracks.size())) {
				this.sendInvalidChoice(str.trim(), prefix, message);
				return true;
			}

			if(!choices.contains(num)) {
				choices.add(num);
			}
		}

		if(botVoiceChannel == null && !musicManager.joinVoiceChannel(userVoiceChannel)) {
			return true;
		}

		for(int choice : choices) {
			AudioTrack track = resultsTracks.get(choice - 1);
			if(musicManager.getScheduler().isPlaying()) {
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + StringUtils.formatTrackName(track.getInfo()) + "** has been added to the playlist.", musicManager.getChannel());
			}
			musicManager.getScheduler().queue(track);
		}

		this.stopWaiting();
		return true;
	}

	private void stopWaiting() {
		cancelTimer.stop();
		resultsTracks.clear();
		MessageManager.removeListener(musicManager.getChannel());
	}

	private void sendInvalidChoice(String choice, String prefix, IMessage message) {
		BotUtils.sendMessage(Emoji.EXCLAMATION + " \"" + choice + "\" is not a valid number. "
				+ "You can use " + prefix + "cancel to cancel the selection.",
				musicManager.getChannel());
		LogUtils.info("{AudioLoadResultListener} {Guild: " + musicManager.getChannel().getGuild().getName()
				+ " (ID: " + musicManager.getChannel().getGuild().getStringID() + ")} Invalid choice: " + message.getContent());
	}
}
