package me.shadorc.shadbot.listener.music;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.discordbot.message.MessageListener;
import me.shadorc.discordbot.message.MessageManager;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.ThreadPoolUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageListener {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private static final ScheduledThreadPoolExecutor EXECUTOR =
			ThreadPoolUtils.newSingleScheduledThreadPoolExecutor("Shadbot-MusicChoiceWaiter-%d");
	private static final int CHOICE_DURATION = 30;

	private final GuildMusicManager musicManager;
	private final IUser userDj;
	private final IVoiceChannel userVoiceChannel;
	private final String identifier;
	private final boolean putFirst;

	private List<AudioTrack> resultsTracks;
	private ScheduledFuture<?> stopWaitingTask;

	public AudioLoadResultListener(GuildMusicManager musicManager, IUser userDj, IVoiceChannel userVoiceChannel, String identifier, boolean putFirst) {
		this.musicManager = musicManager;
		this.userDj = userDj;
		this.userVoiceChannel = userVoiceChannel;
		this.identifier = identifier;
		this.putFirst = putFirst;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		musicManager.joinVoiceChannel(userVoiceChannel, false);
		if(!musicManager.getScheduler().queue(track, putFirst)) {
			BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
					FormatUtils.formatTrackName(track.getInfo())), musicManager.getChannel());
		}
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		List<AudioTrack> tracks = playlist.getTracks();

		// SoundCloud returns an empty playlist when it has not found any results
		if(tracks.isEmpty()) {
			this.onNoMatches();
			return;
		}

		if(identifier.startsWith(YT_SEARCH) || identifier.startsWith(SC_SEARCH)) {
			musicManager.setDj(userDj);
			musicManager.setWaiting(true);

			String choices = FormatUtils.numberedList(5, tracks.size(), count -> String.format("%n%t**%d.** %s",
					count, FormatUtils.formatTrackName(tracks.get(count - 1).getInfo())));

			EmbedBuilder embed = new EmbedBuilder()
					.withAuthorName(String.format("Results (Use %scancel to cancel the selection)",
							Database.getDBGuild(musicManager.getChannel().getGuild()).getPrefix()))
					.withAuthorIcon(musicManager.getDj().getAvatarURL())
					.withColor(Config.BOT_COLOR)
					.withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
					.appendDescription("**Select a music by typing the corresponding number.**"
							+ "\nYou can choose several musics by separating them with a comma."
							+ "\nExample: 1,3,4"
							+ "\n" + choices)
					.withFooterText("This choice will be cancelled in " + CHOICE_DURATION + " seconds.");
			BotUtils.sendMessage(embed.build(), musicManager.getChannel());

			stopWaitingTask = EXECUTOR.schedule(() -> this.stopWaiting(), CHOICE_DURATION, TimeUnit.SECONDS);

			resultsTracks = new ArrayList<>(tracks);
			MessageManager.addListener(musicManager.getChannel(), this);
			return;
		}

		musicManager.joinVoiceChannel(userVoiceChannel, false);

		int count = 0;
		for(AudioTrack track : tracks) {
			musicManager.getScheduler().queue(track, putFirst);
			count++;
			if(musicManager.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE - 1
					&& !PremiumManager.isGuildPremium(musicManager.getChannel().getGuild())
					&& !PremiumManager.isUserPremium(userDj)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, musicManager.getChannel());
				break;
			}
		}

		BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " " + count + " musics have been added to the playlist.", musicManager.getChannel());
	}

	@Override
	public void loadFailed(FriendlyException err) {
		String errMessage = Jsoup.parse(StringUtils.remove(err.getMessage(), "Watch on YouTube")).text().trim();
		BotUtils.sendMessage(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase(), musicManager.getChannel());
		LogUtils.infof("{Guild ID: %d} Load failed: %s", musicManager.getChannel().getGuild().getLongID(), errMessage);

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onNoMatches() {
		BotUtils.sendMessage(TextUtils.noResult(StringUtils.remove(identifier, YT_SEARCH, SC_SEARCH)), musicManager.getChannel());
		LogUtils.infof("{Guild ID: %d} No matches: %s", musicManager.getChannel().getGuild().getLongID(), identifier);

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}

	@Override
	public boolean onMessageReceived(IMessage message) {
		if(!message.getAuthor().equals(musicManager.getDj())) {
			return false;
		}

		String prefix = (String) Database.getDBGuild(musicManager.getChannel().getGuild()).getPrefix();
		if(message.getContent().equalsIgnoreCase(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Choice cancelled.", musicManager.getChannel());
			this.stopWaiting();
			return true;
		}

		String content = message.getContent();

		List<Integer> choices = new ArrayList<>();
		for(String str : content.split(",")) {
			// Remove all non numeric characters
			Integer num = CastUtils.asIntBetween(StringUtils.remove(str, "[^\\d]"), 1, Math.min(5, resultsTracks.size()));
			if(num == null) {
				return false;
			}

			if(!choices.contains(num)) {
				choices.add(num);
			}
		}

		// If the manager was removed from the list while an user chose a music, we re-add it and join voice channel
		GuildMusicManager.putGuildMusicManagerIfAbsent(message.getGuild(), musicManager);
		musicManager.joinVoiceChannel(userVoiceChannel, false);

		for(int choice : choices) {
			AudioTrack track = resultsTracks.get(choice - 1);
			if(musicManager.getScheduler().isPlaying()) {
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + FormatUtils.formatTrackName(track.getInfo())
						+ "** has been added to the playlist.", musicManager.getChannel());
			}
			musicManager.getScheduler().queue(track, putFirst);
			if(musicManager.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE - 1
					&& !PremiumManager.isGuildPremium(musicManager.getChannel().getGuild())
					&& !PremiumManager.isUserPremium(userDj)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, musicManager.getChannel());
				break;
			}
		}

		this.stopWaiting();
		return true;
	}

	private void stopWaiting() {
		stopWaitingTask.cancel(false);
		MessageManager.removeListener(musicManager.getChannel(), this);
		musicManager.setWaiting(false);
		resultsTracks.clear();

		if(musicManager.getScheduler().isStopped()) {
			musicManager.leaveVoiceChannel();
		}
	}
}
