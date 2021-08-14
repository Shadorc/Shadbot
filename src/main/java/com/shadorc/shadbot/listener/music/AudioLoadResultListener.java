package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.core.command.InteractionContext;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.database.DatabaseManager;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.voice.VoiceConnection;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class AudioLoadResultListener implements AudioLoadResultHandler {

    public static final String YT_SEARCH = "ytsearch: ";
    public static final String SC_SEARCH = "scsearch: ";

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final int MAX_PLAYLIST_NAME_LENGTH = 70;

    private final InteractionContext interaction;
    private final Locale locale;
    private final Snowflake guildId;
    private final Snowflake djId;
    private final String identifier;
    private final boolean insertFirst;

    private List<AudioTrack> resultTracks;

    public AudioLoadResultListener(InteractionContext interaction, Locale locale, Snowflake guildId, Snowflake djId,
                                   String identifier, boolean insertFirst) {
        this.interaction = interaction;
        this.locale = locale;
        this.guildId = guildId;
        this.djId = djId;
        this.identifier = identifier;
        this.insertFirst = insertFirst;
    }

    @Override
    public void trackLoaded(AudioTrack audioTrack) {
        LOGGER.debug("{Guild ID: {}} Track loaded: {}", this.guildId.asString(), audioTrack.hashCode());
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .filter(guildMusic -> !guildMusic.getTrackScheduler().startOrQueue(audioTrack, this.insertFirst))
                .flatMap(guildMusic -> this.interaction.createFollowupMessage(Emoji.MUSICAL_NOTE,
                        I18nManager.localize(this.locale, "audioresult.track.loaded")
                                .formatted(FormatUtil.trackName(this.locale, audioTrack.getInfo()))))
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        // SoundCloud returns an empty playlist when no results where found
        if (audioPlaylist.getTracks().isEmpty()) {
            LOGGER.debug("{Guild ID: {}} Empty playlist: {}", this.guildId.asString(), audioPlaylist.hashCode());
            this.onNoMatches();
        }
        // The user is searching something
        else if (audioPlaylist.isSearchResult()) {
            LOGGER.debug("{Guild ID: {}} Playlist loaded, search results: {}", this.guildId.asString(), audioPlaylist.hashCode());
            this.onSearchResult(audioPlaylist);
        }
        // The user loads a full playlist
        else {
            LOGGER.debug("{Guild ID: {}} Playlist loaded, full playlist: {}", this.guildId.asString(), audioPlaylist.hashCode());
            this.onPlaylistLoaded(audioPlaylist);
        }
    }

    private void onSearchResult(AudioPlaylist playlist) {
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMapMany(guildMusic -> {
                    this.resultTracks = playlist.getTracks()
                            .subList(0, Math.min(Config.MUSIC_SEARCHES, playlist.getTracks().size()));

                    guildMusic.setDjId(this.djId);
                    guildMusic.setWaitingForChoice(true);

                    return guildMusic.getDj()
                            .map(User::getAvatarUrl)
                            .map(avatarUrl -> this.formatResultsEmbed(playlist, avatarUrl, this.locale))
                            .flatMap(this.interaction::createFollowupMessage)
                            .flatMapMany(__ ->
                                    // TODO Clean-up: This looks bad
                                    AudioLoadResultMessageInputs.create(guildMusic.getGateway(), Duration.ofSeconds(30),
                                                    guildMusic.getMessageChannelId(), this)
                                            .waitForInputs()
                                            .then(Mono.fromRunnable(() -> guildMusic.setWaitingForChoice(false))));
                })
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private void onPlaylistLoaded(AudioPlaylist playlist) {
        Mono.zip(
                        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId)),
                        DatabaseManager.getPremium().isPremium(this.guildId, this.djId))
                .map(TupleUtils.function((guildMusic, isPremium) -> {
                    final TrackScheduler trackScheduler = guildMusic.getTrackScheduler();
                    final StringBuilder strBuilder = new StringBuilder();

                    int musicsAdded = 0;
                    for (final AudioTrack track : playlist.getTracks()) {
                        trackScheduler.startOrQueue(track, this.insertFirst);
                        musicsAdded++;
                        // The playlist limit is reached and the user / guild is not premium
                        if (trackScheduler.getPlaylist().size() >= Config.PLAYLIST_SIZE - 1 && !isPremium) {
                            strBuilder.append(Emoji.LOCK)
                                    .append(' ')
                                    .append(I18nManager.localize(this.locale, "playlist.limit.reached")
                                            .formatted(Config.PLAYLIST_SIZE, Config.PATREON_URL))
                                    .append('\n');
                            break;
                        }
                    }

                    return strBuilder.append(Emoji.MUSICAL_NOTE)
                            .append(' ')
                            .append(I18nManager.localize(this.locale, "audioresult.playlist.loaded")
                                    .formatted(musicsAdded))
                            .toString();
                }))
                .flatMap(this.interaction::createFollowupMessage)
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private EmbedCreateSpec formatResultsEmbed(AudioPlaylist playlist, String avatarUrl, Locale locale) {
        final String choices = FormatUtil.numberedList(Config.MUSIC_SEARCHES, playlist.getTracks().size(),
                count -> {
                    final AudioTrackInfo info = playlist.getTracks().get(count - 1).getInfo();
                    return "\t**%d.** [%s](%s)".formatted(count, FormatUtil.trackName(this.locale, info), info.uri);
                });

        final String search = playlist.getName().split(":")[1].trim();
        final String abbrSearch = StringUtil.abbreviate(search, MAX_PLAYLIST_NAME_LENGTH);
        return ShadbotUtil.createEmbedBuilder()
                .author(I18nManager.localize(locale, "audioresult.playlist.name").formatted(abbrSearch),
                        null, avatarUrl)
                .thumbnail("https://i.imgur.com/IG3Hj2W.png")
                .description(I18nManager.localize(locale, "audioresult.embed.description").formatted(choices))
                .footer(I18nManager.localize(locale, "audioresult.embed.footer")
                        .formatted(Config.MUSIC_CHOICE_DURATION), null)
                .build();
    }

    @Override
    public void loadFailed(FriendlyException err) {
        LOGGER.debug("{Guild ID: {}} Load failed: {}", this.guildId.asString(), err);
        final String errMessage = ShadbotUtil.cleanLavaplayerErr(err).toLowerCase();
        LOGGER.info("{Guild ID: {}} Load failed: {}", this.guildId.asString(), errMessage);
        this.interaction.createFollowupMessage(Emoji.RED_CROSS,
                        I18nManager.localize(this.locale, "audioresult.load.failed")
                                .formatted(errMessage))
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    @Override
    public void noMatches() {
        LOGGER.debug("{Guild ID: {}} No matches for identifier: {}", this.guildId.asString(), this.identifier);
        this.onNoMatches();
    }

    private void onNoMatches() {
        this.interaction.createFollowupMessage(Emoji.MAGNIFYING_GLASS,
                        I18nManager.localize(this.locale, "audioresult.no.matches")
                                .formatted(StringUtil.remove(this.identifier, YT_SEARCH, SC_SEARCH)))
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private Mono<Void> terminate() {
        return Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(guildMusic -> {
                    guildMusic.removeAudioLoadResultListener(this);
                    // If there is no music playing and nothing is loading, leave the voice channel
                    if (guildMusic.getTrackScheduler().isStopped() && !guildMusic.isWaitingForListeners()) {
                        return guildMusic.getGateway().getVoiceConnectionRegistry()
                                .getVoiceConnection(this.getGuildId())
                                .flatMap(VoiceConnection::disconnect);
                    }
                    return Mono.empty();
                });
    }

    public Snowflake getGuildId() {
        return this.guildId;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public List<AudioTrack> getResultTracks() {
        return Collections.unmodifiableList(this.resultTracks);
    }

}
