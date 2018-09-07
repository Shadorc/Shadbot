package me.shadorc.shadbot.listener.music;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.core.command.CommandManager;
import me.shadorc.shadbot.data.db.DatabaseManager;
import me.shadorc.shadbot.data.premium.PremiumManager;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageInterceptor {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private final GuildMusic guildMusic;
	private final Snowflake djId;
	private final Snowflake voiceChannelId;
	private final String identifier;
	private final boolean putFirst;

	private List<AudioTrack> resultsTracks;
	private Disposable stopWaitingTask;

	public AudioLoadResultListener(GuildMusic guildMusic, Snowflake djId, Snowflake voiceChannelId, String identifier, boolean putFirst) {
		this.guildMusic = guildMusic;
		this.djId = djId;
		this.voiceChannelId = voiceChannelId;
		this.identifier = identifier;
		this.putFirst = putFirst;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		this.guildMusic.joinVoiceChannel(this.voiceChannelId).subscribe();
		if(!this.guildMusic.getScheduler().startOrQueue(track, this.putFirst)) {
			BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
					FormatUtils.formatTrackName(track.getInfo())), this.guildMusic.getMessageChannel())
					.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
					.subscribe();
		}
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		final List<AudioTrack> tracks = playlist.getTracks();

		// SoundCloud returns an empty playlist when no results where found
		if(tracks.isEmpty()) {
			this.onNoMatches();
			return;
		}

		if(this.identifier.startsWith(YT_SEARCH) || this.identifier.startsWith(SC_SEARCH)) {
			this.guildMusic.setDj(this.djId);
			this.guildMusic.setWaitingForChoice(true);

			final String choices = FormatUtils.numberedList(5, tracks.size(),
					count -> String.format("\t**%d.** %s",
							count, FormatUtils.formatTrackName(tracks.get(count - 1).getInfo())));

			DiscordUtils.getAvatarUrl(this.guildMusic.getClient().getUserById(this.guildMusic.getDjId()))
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor("Music results", null, avatarUrl)
							.setThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
							.setDescription("**Select a music by typing the corresponding number.**"
									+ "\nYou can choose several musics by separating them with a comma."
									+ "\nExample: 1,3,4"
									+ "\n\n" + choices)
							.setFooter(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
									DatabaseManager.getDBGuild(this.guildMusic.getGuildId()).getPrefix(), Config.MUSIC_CHOICE_DURATION), null))
					.flatMap(embed -> BotUtils.sendMessage(embed, this.guildMusic.getMessageChannel()))
					.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
					.subscribe();

			this.stopWaitingTask = Mono.delay(Duration.ofSeconds(Config.MUSIC_CHOICE_DURATION))
					.then(Mono.fromRunnable(this::stopWaiting))
					.subscribe();

			this.resultsTracks = new ArrayList<>(tracks);
			MessageInterceptorManager.addInterceptor(this.guildMusic.getMessageChannelId(), this);
			return;
		}

		Flux<Void> flux = Flux.empty();
		flux = flux.concatWith(this.guildMusic.joinVoiceChannel(this.voiceChannelId));

		int musicsAdded = 0;
		for(AudioTrack track : tracks) {
			this.guildMusic.getScheduler().startOrQueue(track, this.putFirst);
			musicsAdded++;
			if(this.guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(this.guildMusic.getGuildId(), this.djId)) {
				flux = flux.concatWith(BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, this.guildMusic.getMessageChannel())
						.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
						.then());
				break;
			}
		}

		flux.concatWith(BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded),
				this.guildMusic.getMessageChannel())
				.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
				.then())
				.subscribe();
	}

	@Override
	public void loadFailed(FriendlyException err) {
		final String errMessage = TextUtils.cleanLavaplayerErr(err);
		BotUtils.sendMessage(Emoji.RED_CROSS + " Sorry, " + errMessage.toLowerCase(), this.guildMusic.getMessageChannel())
				.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
				.subscribe();
		LogUtils.infof("{Guild ID: %d} Load failed: %s", this.guildMusic.getGuildId().asLong(), errMessage);
		this.leaveIfStopped();
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onNoMatches() {
		BotUtils.sendMessage(String.format(Emoji.MAGNIFYING_GLASS + " No results for `%s`.",
				StringUtils.remove(this.identifier, YT_SEARCH, SC_SEARCH)), this.guildMusic.getMessageChannel())
				.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(this.getClass(), this.guildMusic.getGuildId()))
				.subscribe();
		LogUtils.infof("{Guild ID: %d} No matches: %s", this.guildMusic.getGuildId().asLong(), this.identifier);
		this.leaveIfStopped();
	}

	private void leaveIfStopped() {
		if(this.guildMusic.getScheduler().isStopped()) {
			this.guildMusic.leaveVoiceChannel();
		}
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final Message message = event.getMessage();

		// Ignore webhooks and embeds
		if(!message.getAuthorId().isPresent() || !message.getContent().isPresent()) {
			return Mono.just(false);
		}

		final Snowflake authorId = message.getAuthorId().get();
		String content = message.getContent().get().toLowerCase();

		if(!authorId.equals(this.guildMusic.getDjId())) {
			return Mono.just(false);
		}

		final String prefix = DatabaseManager.getDBGuild(this.guildMusic.getGuildId()).getPrefix();
		if(content.equals(prefix + "cancel")) {
			this.stopWaiting();
			return BotUtils.sendMessage(String.format(Emoji.CHECK_MARK + " **%s** cancelled his choice.",
					event.getMember().map(User::getUsername).get()), this.guildMusic.getMessageChannel())
					.thenReturn(true);
		}

		// Remove prefix and command names from message content
		content = StringUtils.remove(content, prefix);
		for(String name : CommandManager.getCommand("play").getNames()) {
			content = StringUtils.remove(content, name);
		}

		final List<Integer> choices = new ArrayList<>();
		for(String choiceStr : content.split(",")) {
			// If the choice is not valid, ignore the message
			final Integer num = NumberUtils.asIntBetween(choiceStr, 1, Math.min(Config.MUSIC_SEARCHES, this.resultsTracks.size()));
			if(num == null) {
				return Mono.just(false);
			}

			if(!choices.contains(num)) {
				choices.add(num);
			}
		}

		// If the manager was removed from the list while an user chose a music, we re-add it and join voice channel again
		GuildMusicManager.GUILD_MUSIC_MAP.putIfAbsent(this.guildMusic.getGuildId(), this.guildMusic);
		final Mono<Void> joinMono = this.guildMusic.joinVoiceChannel(this.voiceChannelId);

		Flux<Message> messages = Flux.empty();
		for(int choice : choices) {
			final AudioTrack track = this.resultsTracks.get(choice - 1);
			if(!this.guildMusic.getScheduler().startOrQueue(track, this.putFirst)) {
				messages = messages.concatWith(BotUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
						FormatUtils.formatTrackName(track.getInfo())), this.guildMusic.getMessageChannel()));
			}

			if(this.guildMusic.getScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE - 1
					&& !PremiumManager.isPremium(this.guildMusic.getGuildId(), authorId)) {
				messages = messages.concatWith(BotUtils.sendMessage(TextUtils.PLAYLIST_LIMIT_REACHED, this.guildMusic.getMessageChannel()));
				break;
			}
		}

		this.stopWaiting();
		return joinMono
				.thenMany(messages)
				.then(Mono.just(true));
	}

	private void stopWaiting() {
		this.stopWaitingTask.dispose();
		MessageInterceptorManager.removeInterceptor(this.guildMusic.getMessageChannelId(), this);
		this.guildMusic.setWaitingForChoice(false);
		this.resultsTracks.clear();
		this.leaveIfStopped();
	}

}
