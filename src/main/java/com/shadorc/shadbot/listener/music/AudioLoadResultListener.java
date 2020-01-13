package com.shadorc.shadbot.listener.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.Shadbot;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.db.DatabaseManager;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.utils.*;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class AudioLoadResultListener implements AudioLoadResultHandler {

    public static final String YT_SEARCH = "ytsearch: ";
    public static final String SC_SEARCH = "scsearch: ";

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
        Mono.justOrEmpty(MusicManager.getInstance().getMusic(this.guildId))
                .filter(guildMusic -> !guildMusic.getTrackScheduler().startOrQueue(audioTrack, this.insertFirst))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(
                        Emoji.MUSICAL_NOTE + " **%s** has been added to the playlist.",
                        FormatUtils.trackName(audioTrack.getInfo())), channel))
                .doOnTerminate(this::terminate)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
    }

    @Override
    public void playlistLoaded(AudioPlaylist audioPlaylist) {
        // SoundCloud returns an empty playlist when no results where found
        if (audioPlaylist.getTracks().isEmpty()) {
            this.onNoMatches();
        }
        // If a track is specifically selected
        else if (audioPlaylist.getSelectedTrack() != null) {
            this.trackLoaded(audioPlaylist.getSelectedTrack());
        }
        // The user is searching something
        else if (audioPlaylist.isSearchResult()) {
            this.onSearchResult(audioPlaylist);
        }
        // The user loads a full playlist
        else {
            this.onPlaylistLoaded(audioPlaylist);
        }
    }

    private void onSearchResult(AudioPlaylist playlist) {
        Mono.justOrEmpty(MusicManager.getInstance().getMusic(this.guildId))
                .flatMapMany(guildMusic -> {
                    this.resultTracks = playlist.getTracks()
                            .subList(0, Math.min(Config.MUSIC_SEARCHES, playlist.getTracks().size()));

                    guildMusic.setDj(this.djId);
                    guildMusic.setWaitingForChoice(true);

                    return guildMusic.getClient().getUserById(guildMusic.getDjId())
                            .map(User::getAvatarUrl)
                            .flatMap(avatarUrl -> guildMusic.getMessageChannel()
                                    .flatMap(channel -> DiscordUtils.sendMessage(this.getPlaylistEmbed(playlist, avatarUrl), channel)))
                            .flatMapMany(ignored -> new AudioLoadResultInputs(guildMusic.getClient(), Duration.ofSeconds(30), this)
                                    .waitForInputs()
                                    .then(Mono.fromRunnable(() -> guildMusic.setWaitingForChoice(false))));
                })
                .doOnTerminate(this::terminate)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
    }

    private void onPlaylistLoaded(AudioPlaylist playlist) {
        Mono.justOrEmpty(MusicManager.getInstance().getMusic(this.guildId))
                .flatMap(guildMusic -> {
                    final StringBuilder strBuilder = new StringBuilder();

                    int musicsAdded = 0;
                    for (final AudioTrack track : playlist.getTracks()) {
                        guildMusic.getTrackScheduler().startOrQueue(track, this.insertFirst);
                        musicsAdded++;
                        // The playlist limit is reached and the user / guild is not premium
                        if (guildMusic.getTrackScheduler().getPlaylist().size() >= Config.PLAYLIST_SIZE - 1
                                && !DatabaseManager.getPremium().isGuildPremium(this.guildId)
                                && !DatabaseManager.getPremium().isUserPremium(this.djId)) {
                            strBuilder.append(TextUtils.PLAYLIST_LIMIT_REACHED + "\n");
                            break;
                        }
                    }

                    strBuilder.append(String.format(Emoji.MUSICAL_NOTE + " %d musics have been added to the playlist.", musicsAdded));
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(strBuilder.toString(), channel));
                })
                .doOnTerminate(this::terminate)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
    }

    private Consumer<EmbedCreateSpec> getPlaylistEmbed(AudioPlaylist playlist, String avatarUrl) {
        final String choices = FormatUtils.numberedList(Config.MUSIC_SEARCHES, playlist.getTracks().size(),
                count -> {
                    final AudioTrackInfo info = playlist.getTracks().get(count - 1).getInfo();
                    return String.format("\t**%d.** [%s](%s)", count, FormatUtils.trackName(info), info.uri);
                });

        final String playlistName = org.apache.commons.lang3.StringUtils.abbreviate(playlist.getName(), MAX_PLAYLIST_NAME_LENGTH);
        return DiscordUtils.getDefaultEmbed()
                .andThen(embed -> embed.setAuthor(playlistName, null, avatarUrl)
                        .setThumbnail("https://i.imgur.com/IG3Hj2W.png")
                        .setDescription("**Select a music by typing the corresponding number.**"
                                + "\nYou can choose several musics by separating them with a comma."
                                + "\nExample: 1,3,4"
                                + "\n\n" + choices)
                        .setFooter(String.format("Use %scancel to cancel the selection (Automatically canceled in %ds).",
                                DatabaseManager.getGuilds().getDBGuild(this.guildId).getSettings().getPrefix(), Config.MUSIC_CHOICE_DURATION), null));
    }

    @Override
    public void loadFailed(FriendlyException e) {
        Mono.justOrEmpty(MusicManager.getInstance().getMusic(this.guildId))
                .flatMap(guildMusic -> {
                    final String errMessage = TextUtils.cleanLavaplayerErr(e);
                    LogUtils.info("{Guild ID: %d} Load failed: %s", this.guildId.asLong(), errMessage);
                    return guildMusic.getMessageChannel()
                            .flatMap(channel -> DiscordUtils.sendMessage(
                                    String.format(Emoji.RED_CROSS + " Something went wrong while loading the track: %s",
                                            errMessage.toLowerCase()), channel));
                })
                .doOnTerminate(this::terminate)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, thr -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), thr));
    }

    @Override
    public void noMatches() {
        this.onNoMatches();
    }

    private void onNoMatches() {
        Mono.justOrEmpty(MusicManager.getInstance().getMusic(this.guildId))
                .flatMap(GuildMusic::getMessageChannel)
                .flatMap(channel -> DiscordUtils.sendMessage(String.format(Emoji.MAGNIFYING_GLASS + " No results for `%s`.",
                        StringUtils.remove(this.identifier, YT_SEARCH, SC_SEARCH)), channel))
                .doOnTerminate(this::terminate)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(Shadbot.getClient(), err));
    }

    private void terminate() {
        final GuildMusic guildMusic = MusicManager.getInstance().getMusic(this.guildId);
        if (guildMusic != null) {
            guildMusic.removeAudioLoadResultListener(this);
        }
    }

    public Snowflake getGuildId() {
        return this.guildId;
    }

    public List<AudioTrack> getResultTracks() {
        return Collections.unmodifiableList(this.resultTracks);
    }

}
