package me.shadorc.shadbot.listener.music;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jsoup.Jsoup;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.message.MessageListener;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.CastUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.EmbedBuilder;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageListener {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private static final int CHOICE_DURATION = 30;

	private final GuildMusic guildMusic;
	private final IUser userDj;
	private final IVoiceChannel userVoiceChannel;
	private final String identifier;
	private final boolean putFirst;

	private List<AudioTrack> resultsTracks;
	private ScheduledFuture<?> stopWaitingTask;

	public AudioLoadResultListener(GuildMusic guildMusic, IUser userDj, IVoiceChannel userVoiceChannel, String identifier, boolean putFirst) {
		this.guildMusic = guildMusic;
		this.userDj = userDj;
		this.userVoiceChannel = userVoiceChannel;
		this.identifier = identifier;
		this.putFirst = putFirst;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		guildMusic.joinVoiceChannel(userVoiceChannel);
		if(!guildMusic.getScheduler().startOrQueue(track, putFirst)) {
			BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
					FormatUtils.formatTrackName(track.getInfo())), guildMusic.getChannel());
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
			guildMusic.setDj(userDj);
			guildMusic.setWaiting(true);

			String choices = FormatUtils.numberedList(5, tracks.size(), count -> String.format("\t**%d.** %s",
					count, FormatUtils.formatTrackName(tracks.get(count - 1).getInfo())));

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("Results")
					.withAuthorIcon(guildMusic.getDj().getAvatarURL())
					.withThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
					.appendDescription("**Select a music by typing the corresponding number.**"
							+ "\nYou can choose several musics by separating them with a comma."
							+ "\nExample: 1,3,4"
							+ "\n\n" + choices)
					.withFooterText(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
							Database.getDBGuild(guildMusic.getChannel().getGuild()).getPrefix(), CHOICE_DURATION));
			BotUtils.sendMessage(embed.build(), guildMusic.getChannel());

			stopWaitingTask = Shadbot.getScheduler().schedule(() -> this.stopWaiting(), CHOICE_DURATION, TimeUnit.SECONDS);

			resultsTracks = new ArrayList<>(tracks);
			MessageManager.addListener(guildMusic.getChannel(), this);
			return;
		}

		guildMusic.joinVoiceChannel(userVoiceChannel);

		int musicsAdded = 0;
		for(AudioTrack track : tracks) {
			guildMusic.getScheduler().startOrQueue(track, putFirst);
			musicsAdded++;
			if(guildMusic.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(guildMusic.getChannel().getGuild(), userDj)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, guildMusic.getChannel());
				break;
			}
		}

		BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded), guildMusic.getChannel());
	}

	@Override
	public void loadFailed(FriendlyException err) {
		String errMessage = Jsoup.parse(StringUtils.remove(err.getMessage(), "Watch on YouTube")).text().trim();
		BotUtils.sendMessage(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase(), guildMusic.getChannel());
		LogUtils.infof("{Guild ID: %d} Load failed: %s", guildMusic.getChannel().getGuild().getLongID(), errMessage);

		if(guildMusic.getScheduler().isStopped()) {
			guildMusic.leaveVoiceChannel();
		}
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onNoMatches() {
		BotUtils.sendMessage(TextUtils.noResult(StringUtils.remove(identifier, YT_SEARCH, SC_SEARCH)), guildMusic.getChannel());
		LogUtils.infof("{Guild ID: %d} No matches: %s", guildMusic.getChannel().getGuild().getLongID(), identifier);

		if(guildMusic.getScheduler().isStopped()) {
			guildMusic.leaveVoiceChannel();
		}
	}

	@Override
	public boolean intercept(IMessage message) {
		if(!message.getAuthor().equals(guildMusic.getDj())) {
			return false;
		}

		String prefix = Database.getDBGuild(guildMusic.getChannel().getGuild()).getPrefix();
		if(message.getContent().equalsIgnoreCase(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Choice cancelled.", guildMusic.getChannel());
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
		GuildMusicManager.GUILD_MUSIC_MAP.putIfAbsent(message.getGuild().getLongID(), guildMusic);
		guildMusic.joinVoiceChannel(userVoiceChannel);

		for(int choice : choices) {
			AudioTrack track = resultsTracks.get(choice - 1);
			if(guildMusic.getScheduler().isPlaying()) {
				BotUtils.sendMessage(Emoji.MUSICAL_NOTE + " **" + FormatUtils.formatTrackName(track.getInfo())
						+ "** has been added to the playlist.", guildMusic.getChannel());
			}
			guildMusic.getScheduler().startOrQueue(track, putFirst);
			if(guildMusic.getScheduler().getPlaylist().size() >= Config.MAX_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(guildMusic.getChannel().getGuild(), userDj)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, guildMusic.getChannel());
				break;
			}
		}

		this.stopWaiting();
		return true;
	}

	private void stopWaiting() {
		stopWaitingTask.cancel(false);
		MessageManager.removeListener(guildMusic.getChannel(), this);
		guildMusic.setWaiting(false);
		resultsTracks.clear();

		if(guildMusic.getScheduler().isStopped()) {
			guildMusic.leaveVoiceChannel();
		}
	}
}
