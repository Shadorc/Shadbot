package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.core.i18n.I18nManager;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.db.guilds.entity.DBGuild;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.music.TrackScheduler;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.ExceptionHandler;
import com.shadorc.shadbot.utils.DiscordUtil;
import com.shadorc.shadbot.utils.FormatUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.shadorc.shadbot.music.MusicManager.LOGGER;

public class AudioLoadResultListener implements AudioLoadResultHandler {

    public static final String YT_SEARCH = "ytsearch: ";
    public static final String SC_SEARCH = "scsearch: ";

    private static final Scheduler DEFAULT_SCHEDULER = Schedulers.boundedElastic();
    private static final int MAX_PLAYLIST_NAME_LENGTH = 70;

    private final Snowflake guildId;
    private final Snowflake djId;
    private final String identifier;
    private final boolean insertFirst;

    private List<AudioTrack> resultTracks;

    public AudioLoadResultListener(Snowflake guildId, Snowflake djId, String identifier, boolean insertFirst) {
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
                .flatMap(GuildMusic::getMessageChannel)
                .zipWith(DatabaseManager.getGuilds().getDBGuild(this.guildId)
                        .map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((channel, locale) -> DiscordUtil.sendMessage(Emoji.MUSICAL_NOTE,
                        I18nManager.localize(locale, "audioresult.track.loaded")
                                .formatted(FormatUtil.trackName(audioTrack.getInfo())),
                        channel)))
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
        // If a track is specifically selected
        // TODO: Do we want to load only one track?
        else if (audioPlaylist.getSelectedTrack() != null) {
            LOGGER.debug("{Guild ID: {}} Playlist loaded, track selected: {}", this.guildId.asString(), audioPlaylist.hashCode());
            this.trackLoaded(audioPlaylist.getSelectedTrack());
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

                    return guildMusic.getGateway()
                            .getUserById(guildMusic.getDjId())
                            .map(User::getAvatarUrl)
                            .zipWith(DatabaseManager.getGuilds().getDBGuild(this.guildId).map(DBGuild::getLocale))
                            .map(TupleUtils.function((avatarUrl, locale) ->
                                    AudioLoadResultListener.formatResultsEmbed(playlist, avatarUrl, locale)))
                            .flatMap(embed -> guildMusic.getMessageChannel()
                                    .flatMap(channel -> DiscordUtil.sendMessage(embed, channel)))
                            .flatMapMany(__ ->
                                    // TODO: This seems bad
                                    AudioLoadResultInputs.create(guildMusic.getGateway(), Duration.ofSeconds(30),
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
                DatabaseManager.getPremium().isPremium(this.guildId, this.djId),
                DatabaseManager.getGuilds().getDBGuild(this.guildId).map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((guildMusic, isPremium, locale) -> {
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
                                    .append(I18nManager.localize(locale, "playlist.limit.reached")
                                            .formatted(Config.PLAYLIST_SIZE, Config.PATREON_URL))
                                    .append('\n');
                            break;
                        }
                    }

                    strBuilder.append(Emoji.MUSICAL_NOTE)
                            .append(' ')
                            .append(I18nManager.localize(locale, "audioresult.playlist.loaded")
                                    .formatted(musicsAdded));

                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtil.sendMessage(strBuilder.toString(), channel));
                }))
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private static Consumer<EmbedCreateSpec> formatResultsEmbed(AudioPlaylist playlist, String avatarUrl, Locale locale) {
        final String choices = FormatUtil.numberedList(Config.MUSIC_SEARCHES, playlist.getTracks().size(),
                count -> {
                    final AudioTrackInfo info = playlist.getTracks().get(count - 1).getInfo();
                    return "\t**%d.** [%s](%s)".formatted(count, FormatUtil.trackName(info), info.uri);
                });

        final String playlistName = StringUtil.abbreviate(playlist.getName(), MAX_PLAYLIST_NAME_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(playlistName, null, avatarUrl)
                        .setThumbnail("https://i.imgur.com/IG3Hj2W.png")
                        .setDescription(I18nManager.localize(locale, "audioresult.embed.description").formatted(choices))
                        .setFooter(I18nManager.localize(locale, "audioresult.embed.footer")
                                .formatted(Config.MUSIC_CHOICE_DURATION), null));
    }

    @Override
    public void loadFailed(FriendlyException err) {
        LOGGER.debug("{Guild ID: {}} Load failed: {}", this.guildId.asString(), err);
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .zipWith(DatabaseManager.getGuilds().getDBGuild(this.guildId).map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((guildMusic, locale) -> {
                    final String errMessage = ShadbotUtil.cleanLavaplayerErr(err);
                    LOGGER.info("{Guild ID: {}} Load failed: {}", this.guildId.asString(), errMessage);
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtil.sendMessage(Emoji.RED_CROSS,
                                    I18nManager.localize(locale, "audioresult.load.failed")
                                            .formatted(errMessage.toLowerCase()), channel));
                }))
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
        Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .zipWith(DatabaseManager.getGuilds().getDBGuild(this.guildId).map(DBGuild::getLocale))
                .flatMap(TupleUtils.function((channel, locale) -> DiscordUtil.sendMessage(Emoji.MAGNIFYING_GLASS,
                        I18nManager.localize(locale, "audioresult.no.matches")
                                .formatted(StringUtil.remove(this.identifier, YT_SEARCH, SC_SEARCH)), channel)))
                .then(this.terminate())
                .subscribeOn(DEFAULT_SCHEDULER)
                .subscribe(null, ExceptionHandler::handleUnknownError);
    }

    private Mono<Void> terminate() {
        return Mono.justOrEmpty(MusicManager.getGuildMusic(this.guildId))
                .flatMap(guildMusic -> guildMusic.removeAudioLoadResultListener(this));
    }

    public Snowflake getGuildId() {
        return this.guildId;
    }

    public List<AudioTrack> getResultTracks() {
        return Collections.unmodifiableList(this.resultTracks);
    }

}
