package me.shadorc.shadbot.listener.music;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.db.Database;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.message.MessageListener;
import me.shadorc.shadbot.message.MessageManager;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageListener {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private final GuildMusic guildMusic;
	private final Snowflake djId;
	private final Snowflake voiceChannelId;
	private final String identifier;
	private final boolean putFirst;

	private List<AudioTrack> resultsTracks;
	private ScheduledFuture<?> stopWaitingTask;

	public AudioLoadResultListener(GuildMusic guildMusic, Snowflake djId, Snowflake voiceChannelId, String identifier, boolean putFirst) {
		this.guildMusic = guildMusic;
		this.djId = djId;
		this.voiceChannelId = voiceChannelId;
		this.identifier = identifier;
		this.putFirst = putFirst;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		guildMusic.joinVoiceChannel(voiceChannelId);
		if(!guildMusic.getScheduler().startOrQueue(track, putFirst)) {
			BotUtils.sendMessage(
					String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
							FormatUtils.formatTrackName(track.getInfo())),
					guildMusic.getMessageChannel());
		}
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		List<AudioTrack> tracks = playlist.getTracks();

		// SoundCloud returns an empty playlist when no results where found
		if(tracks.isEmpty()) {
			this.onNoMatches();
			return;
		}

		if(identifier.startsWith(YT_SEARCH) || identifier.startsWith(SC_SEARCH)) {
			guildMusic.setDj(djId);
			guildMusic.setWaiting(true);

			String choices = FormatUtils.numberedList(5, tracks.size(),
					count -> String.format("\t**%d.** %s",
							count, FormatUtils.formatTrackName(tracks.get(count - 1).getInfo())));

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed()
					// TODO "Music results", null, guildMusic.getDj().getAvatarURL())
					.setThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
					.setDescription("**Select a music by typing the corresponding number.**"
							+ "\nYou can choose several musics by separating them with a comma."
							+ "\nExample: 1,3,4"
							+ "\n\n" + choices)
					.setFooter(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
							Database.getDBGuild(guildMusic.getGuildId()).getPrefix(), Config.MUSIC_CHOICE_DURATION), null);
			BotUtils.sendMessage(embed, guildMusic.getMessageChannel());

			stopWaitingTask = GuildMusicManager.VOICE_LEAVE_SCHEDULER
					.schedule(this::stopWaiting, Config.MUSIC_CHOICE_DURATION, TimeUnit.SECONDS);

			resultsTracks = new ArrayList<>(tracks);
			MessageManager.addListener(guildMusic.getMessageChannelId(), this);
			return;
		}

		guildMusic.joinVoiceChannel(voiceChannelId);

		int musicsAdded = 0;
		for(AudioTrack track : tracks) {
			guildMusic.getScheduler().startOrQueue(track, putFirst);
			musicsAdded++;
			if(guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(guildMusic.getGuildId(), djId)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, guildMusic.getMessageChannel());
				break;
			}
		}

		BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded),
				guildMusic.getMessageChannel());
	}

	@Override
	public void loadFailed(FriendlyException err) {
		String errMessage = TextUtils.cleanLavaplayerErr(err);
		BotUtils.sendMessage(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase(), guildMusic.getMessageChannel());
		LogUtils.infof("{Guild ID: %d} Load failed: %s", guildMusic.getGuildId().asLong(), errMessage);
		this.leaveIfStopped();
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onNoMatches() {
		BotUtils.sendMessage(TextUtils.noResult(StringUtils.remove(identifier, YT_SEARCH, SC_SEARCH)), guildMusic.getMessageChannel());
		LogUtils.infof("{Guild ID: %d} No matches: %s", guildMusic.getGuildId(), identifier);
		this.leaveIfStopped();
	}

	private void leaveIfStopped() {
		if(guildMusic.getScheduler().isStopped()) {
			guildMusic.leaveVoiceChannel();
		}
	}

	@Override
	public boolean intercept(Snowflake guildId, Message message) {
		// Ignore webhooks
		if(!message.getAuthorId().isPresent() || !message.getContent().isPresent()) {
			return false;
		}

		Snowflake authorId = message.getAuthorId().get();
		String content = message.getContent().get().toLowerCase();

		if(!authorId.equals(guildMusic.getDjId())) {
			return false;
		}

		String prefix = Database.getDBGuild(guildMusic.getGuildId()).getPrefix();
		if(content.equals(prefix + "cancel")) {
			BotUtils.sendMessage(Emoji.CHECK_MARK + " Choice cancelled.", guildMusic.getMessageChannel());
			this.stopWaiting();
			return true;
		}

		// Remove prefix and command names from message content
		content = StringUtils.remove(content, prefix);
		for(String name : CommandManager.getCommand("play").getNames()) {
			content = StringUtils.remove(content, name);
		}

		List<Integer> choices = new ArrayList<>();
		for(String str : content.split(",")) {
			Integer num = NumberUtils.asIntBetween(str, 1, Math.min(Config.MUSIC_SEARCHES, resultsTracks.size()));
			if(num == null) {
				return false;
			}

			if(!choices.contains(num)) {
				choices.add(num);
			}
		}

		// If the manager was removed from the list while an user chose a music, we re-add it and join voice channel
		GuildMusicManager.GUILD_MUSIC_MAP.putIfAbsent(guildId, guildMusic);
		guildMusic.joinVoiceChannel(voiceChannelId);

		for(int choice : choices) {
			AudioTrack track = resultsTracks.get(choice - 1);
			if(guildMusic.getScheduler().isPlaying()) {
				BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
						FormatUtils.formatTrackName(track.getInfo())), guildMusic.getMessageChannel());
			}
			guildMusic.getScheduler().startOrQueue(track, putFirst);
			if(guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(guildMusic.getGuildId(), authorId)) {
				BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, guildMusic.getMessageChannel());
				break;
			}
		}

		this.stopWaiting();
		return true;
	}

	private void stopWaiting() {
		stopWaitingTask.cancel(false);
		MessageManager.removeListener(guildMusic.getMessageChannelId(), this);
		guildMusic.setWaiting(false);
		resultsTracks.clear();
		this.leaveIfStopped();
	}

}
