package me.shadorc.shadbot.listener.music;

import java.time.Duration;
import java.util.ArrayList;
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

	private static final int MAX_RESULTS = 5;
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
		this.guildMusic.joinVoiceChannel(this.voiceChannelId);
		if(!this.guildMusic.getTrackScheduler().startOrQueue(track, this.putFirst)) {
			this.guildMusic.getMessageChannel()
					.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
							FormatUtils.trackName(track.getInfo())), channel))
					.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err)))
					.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err));
		}
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
		final String errMessage = TextUtils.cleanLavaplayerErr(err);
		LogUtils.info("{Guild ID: %d} Load failed: %s", this.guildMusic.getGuildId().asLong(), errMessage);
		this.guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.RED_CROSS + " Sorry, %s", errMessage.toLowerCase()), channel))
				.onErrorResume(thr -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), thr)))
				.subscribe(null, thr -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), thr));
		this.leaveIfStopped();
	}

	@Override
	public void noMatches() {
		this.onNoMatches();
	}

	private void onSearchResult(AudioPlaylist playlist) {
		this.guildMusic.setDj(this.djId);
		this.guildMusic.setWaitingForChoice(true);

		final String choices = FormatUtils.numberedList(MAX_RESULTS, playlist.getTracks().size(),
				count -> {
					final AudioTrackInfo info = playlist.getTracks().get(count - 1).getInfo();
					return String.format("\t**%d.** [%s](%s)", count, FormatUtils.trackName(info), info.uri);
				});
		
		this.guildMusic.getClient().getUserById(this.guildMusic.getDjId())
				.map(User::getAvatarUrl)
				.map(avatarUrl -> { 
					final Consumer<EmbedCreateSpec> embedConsumer = embed -> {
						EmbedUtils.getDefaultEmbed().accept(embed);
						embed.setAuthor(playlist.getName(), null, avatarUrl);
						embed.setThumbnail("http://icons.iconarchive.com/icons/dtafalonso/yosemite-flat/512/Music-icon.png");
						embed.setDescription("**Select a music by typing the corresponding number.**"
									+ "\nYou can choose several musics by separating them with a comma."
									+ "\nExample: 1,3,4"
									+ "\n\n" + choices);
						embed.setFooter(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
									Shadbot.getDatabase().getDBGuild(this.guildMusic.getGuildId()).getPrefix(), Config.MUSIC_CHOICE_DURATION), null);
					};
					
					return embedConsumer;
				})
				.flatMap(embedConsumer -> this.guildMusic.getMessageChannel()
						.flatMap(channel -> DiscordUtils.sendMessage(embedConsumer, channel)))
				.then(Mono.fromRunnable(() -> {
					this.stopWaitingTask = Mono.delay(Duration.ofSeconds(Config.MUSIC_CHOICE_DURATION))
							.then(Mono.fromRunnable(this::stopWaiting))
							.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err)))
							.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err));

					this.resultsTracks = new ArrayList<>(playlist.getTracks().subList(0, Math.min(MAX_RESULTS, playlist.getTracks().size())));
					MessageInterceptorManager.addInterceptor(this.guildMusic.getMessageChannelId(), this);
				}))
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err));
	}

	private void onPlaylistLoaded(AudioPlaylist playlist) {
		this.guildMusic.joinVoiceChannel(this.voiceChannelId);

		final StringBuilder strBuilder = new StringBuilder();
		int musicsAdded = 0;
		for(final AudioTrack track : playlist.getTracks()) {
			this.guildMusic.getTrackScheduler().startOrQueue(track, this.putFirst);
			musicsAdded++;
			// The playlist limit is reached and the user / guild is not premium
			if(this.guildMusic.getTrackScheduler().getPlaylist().size() >= Config.DEFAULT_PLAYLIST_SIZE
					&& !Shadbot.getPremium().isPremium(this.guildMusic.getGuildId(), this.djId)) {
				strBuilder.append(TextUtils.PLAYLIST_LIMIT_REACHED + "\n");
				break;
			}
		}

		strBuilder.append(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded));
		this.guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel))
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err));
	}

	private void onNoMatches() {
		this.guildMusic.getMessageChannel()
				.flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MAGNIFYING_GLASS + " No results for `%s`.",
						StringUtils.remove(this.identifier, YT_SEARCH, SC_SEARCH)), channel))
				.onErrorResume(err -> Mono.fromRunnable(() -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err)))
				.subscribe(null, err -> ExceptionHandler.handleUnknownError(this.guildMusic.getClient(), err));
		this.leaveIfStopped();
	}

	private void leaveIfStopped() {
		if(this.guildMusic.getTrackScheduler().isStopped()) {
			this.guildMusic.leaveVoiceChannel();
		}
	}

	@Override
	public Mono<Boolean> isIntercepted(MessageCreateEvent event) {
		return Mono.justOrEmpty(event.getMember())
				.filter(member -> member.getId().equals(this.guildMusic.getDjId()))
				.filter(member -> event.getMessage().getContent().isPresent())
				.map(Member::getUsername)
				.flatMap(username -> {
					final String content = event.getMessage().getContent().get();
					final String prefix = Shadbot.getDatabase().getDBGuild(this.guildMusic.getGuildId()).getPrefix();
					if(content.equals(String.format("%scancel", prefix))) {
						this.stopWaiting();
						return this.guildMusic.getMessageChannel()
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
						final Integer num = NumberUtils.asIntBetween(choice, 1, Math.min(Config.MUSIC_SEARCHES, this.resultsTracks.size()));
						if(num == null) {
							return Mono.just(false);
						}

						choices.add(num);
					}

					choices.stream().forEach(choice -> this.trackLoaded(this.resultsTracks.get(choice - 1)));

					this.stopWaiting();
					return Mono.just(true);
				});
	}

	private void stopWaiting() {
		this.stopWaitingTask.dispose();
		MessageInterceptorManager.removeInterceptor(this.guildMusic.getMessageChannelId(), this);
		this.guildMusic.setWaitingForChoice(false);
		this.leaveIfStopped();
	}

}
