package me.shadorc.shadbot.listener.music;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.command.CommandInitializer;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptor;
import me.shadorc.shadbot.listener.interceptor.MessageInterceptorManager;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.NumberUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;
import me.shadorc.shadbot.utils.object.Emoji;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public class AudioLoadResultListener implements AudioLoadResultHandler, MessageInterceptor {

	public static final String YT_SEARCH = "ytsearch: ";
	public static final String SC_SEARCH = "scsearch: ";

	private static final int MAX_RESULTS = 5;

	private final Snowflake guildId;
	private final Snowflake djId;
	private final String identifier;
	private final boolean putFirst;

	private List<AudioTrack> resultsTracks;
	private Disposable stopWaitingTask;

	public AudioLoadResultListener(Snowflake guildId, Snowflake djId, String identifier, boolean putFirst) {
		this.guildId = guildId;
		this.djId = djId;
		this.identifier = identifier;
		this.putFirst = putFirst;
	}

	@Override
	public void trackLoaded(AudioTrack track) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}
		guildMusic.joinVoiceChannel();
		if(!guildMusic.getTrackScheduler().startOrQueue(track, this.putFirst)) {
			guildMusic.getMessageChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
							FormatUtils.trackName(track.getInfo())), channel))
					.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
		}

		this.terminate();
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		// SoundCloud returns an empty playlist when no results where found
		if(playlist.getTracks().isEmpty()) {
			this.onNoMatches();
		}
		// If a track is specifically selected
		else if(playlist.getSelectedTrack() != null) {
			this.trackLoaded(playlist.getSelectedTrack());
		}
		// The user is searching something
		else if(playlist.isSearchResult()) {
			this.onSearchResult(playlist);
		}
		// The user loads a full playlist
		else {
			this.onPlaylistLoaded(playlist);
		}
	}

	@Override
	public void loadFailed(FriendlyException err) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}
		final String errMessage = TextUtils.cleanLavaplayerErr(err);
		LogUtils.info("{Guild ID: %d} Load failed: %s", this.guildId.asLong(), errMessage);
		guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.RED_CROSS + " Sorry, %s", errMessage.toLowerCase()), channel))
				.subscribe(null, thr -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), thr));

		this.terminate();
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onNoMatches() {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}
		guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MAGNIFYING_GLASS + " No results for `%s`.",
						StringUtils.remove(this.identifier, YT_SEARCH, SC_SEARCH)), channel))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));

		this.terminate();
	}

	private void onSearchResult(AudioPlaylist playlist) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}
		guildMusic.setDj(this.djId);
		guildMusic.setWaitingForChoice(true);

		guildMusic.getClient()
				.getUserById(guildMusic.getDjId())
				.map(User::getAvatarUrl)
				.flatMap(avatarUrl -> guildMusic.getMessageChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(this.getPlaylistEmbed(playlist, avatarUrl), channel)))
				.map(ignored -> {
					this.resultsTracks = playlist.getTracks().subList(0, Math.min(MAX_RESULTS, playlist.getTracks().size()));
					MessageInterceptorManager.addInterceptor(guildMusic.getMessageChannelId(), this);

					return Mono.delay(Duration.ofSeconds(Config.MUSIC_CHOICE_DURATION))
							.doOnNext(tick -> this.terminate())
							.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
				})
				.doOnNext(stopWaitingTask -> this.stopWaitingTask = stopWaitingTask)
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));
	}

	private Consumer<EmbedCreateSpec> getPlaylistEmbed(AudioPlaylist playlist, String avatarUrl) {
		final String playlistName = playlist.getName();
		final String name = playlistName == null || playlistName.isBlank() ? "Playlist" : playlistName;

		final String choices = FormatUtils.numberedList(MAX_RESULTS, playlist.getTracks().size(),
				count -> {
					final AudioTrackInfo info = playlist.getTracks().get(count - 1).getInfo();
					return String.format("\t**%d.** [%s](%s)", count, FormatUtils.trackName(info), info.uri);
				});

		return EmbedUtils.getDefaultEmbed()
				.andThen(embed -> embed.setAuthor(name, null, avatarUrl)
						.setThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png")
						.setDescription("**Select a music by typing the corresponding number.**"
								+ "\nYou can choose several musics by separating them with a comma."
								+ "\nExample: 1,3,4"
								+ "\n\n" + choices)
						.setFooter(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
								Shadbot.getDatabase().getDBGuild(this.guildId).getPrefix(), Config.MUSIC_CHOICE_DURATION), null));
	}

	private void onPlaylistLoaded(AudioPlaylist playlist) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return;
		}
		guildMusic.joinVoiceChannel();

		final StringBuilder strBuilder = new StringBuilder();
		int musicsAdded = 0;
		for(final AudioTrack track : playlist.getTracks()) {
			guildMusic.getTrackScheduler().startOrQueue(track, this.putFirst);
			musicsAdded++;
			// The playlist limit is reached and the user / guild is not premium
			if(guildMusic.getTrackScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE
					&& !Shadbot.getPremium().isPremium(this.guildId, this.djId)) {
				strBuilder.append(TextUtils.PLAYLIST_LIMIT_REACHED + "\n");
				break;
			}
		}

		strBuilder.append(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded));
		guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(guildMusic.getClient(), err));

		this.terminate();
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		if(guildMusic == null) {
			return Mono.just(false);
		}
		return Mono.justOrEmpty(event.getMember())
				.filter(member -> member.getId().equals(guildMusic.getDjId()))
				.filter(member -> event.getMessage().getContent().isPresent())
				.map(Member::getUsername)
				.flatMap(username -> {
					final String content = event.getMessage().getContent().get();
					final String prefix = Shadbot.getDatabase().getDBGuild(this.guildId).getPrefix();
					if(content.equals(String.format("%scancel", prefix))) {
						guildMusic.setWaitingForChoice(false);
						this.terminate();
						return guildMusic.getMessageChannel()
								.flatMap(channel -> DiscordUtils.sendMessage(
										String.format(Emoji.CHECK_MARK + " **%s** cancelled his choice.", username), channel))
								.thenReturn(true);
					}

					// Remove prefix and command names from message content
					String contentCleaned = StringUtils.remove(content, prefix);
					contentCleaned = StringUtils.remove(contentCleaned, CommandInitializer.getCommand("play").getNames().toArray(new String[0]));

					final Set<Integer> choices = new HashSet<>();
					for(final String choice : contentCleaned.split(",")) {
						// If the choice is not valid, ignore the message
						final Integer num = NumberUtils.asIntBetween(choice.trim(), 1, Math.min(Config.MUSIC_SEARCHES, this.resultsTracks.size()));
						if(num == null) {
							return Mono.just(false);
						}

						choices.add(num);
					}

					choices.stream().forEach(choice -> this.trackLoaded(this.resultsTracks.get(choice - 1)));

					guildMusic.setWaitingForChoice(false);
					this.terminate();
					return Mono.just(true);
				});
	}

	public void terminate() {
		if(this.stopWaitingTask != null) {
			this.stopWaitingTask.dispose();
		}
		final GuildMusic guildMusic = GuildMusicManager.get(this.guildId);
		MessageInterceptorManager.removeInterceptor(guildMusic.getMessageChannelId(), this);
		guildMusic.removeAudioLoadResultListener(this);
		if(guildMusic.getTrackScheduler().isStopped()) {
			guildMusic.leaveVoiceChannel();
		}
	}

}
