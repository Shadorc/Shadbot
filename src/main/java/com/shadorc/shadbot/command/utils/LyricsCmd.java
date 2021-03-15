package com.shadorc.shadbot.command.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtil;
import com.shadorc.shadbot.utils.ShadbotUtil;
import com.shadorc.shadbot.utils.StringUtil;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.select.Elements;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class LyricsCmd extends BaseCmd {

    private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;
    private static final int MAX_TITLE_LENGTH = Embed.MAX_TITLE_LENGTH / 3;
    private static final int MAX_RETRY = 5;

    private static final String HOME_URL = "https://www.musixmatch.com";
    // Make html() preserve linebreak and spacing
    private static final OutputSettings PRESERVE_FORMAT = new OutputSettings().prettyPrint(false);
    private static final Pattern PATTERN = Pattern.compile("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)");
    private static final Supplier<CommandException> NO_TRACK_EXCEPTION = () -> new CommandException(
            "You are currently not listening to music, please provide a music name to search.");

    public LyricsCmd() {
        super(CommandCategory.UTILS, "lyrics", "Search for music lyrics");
        this.addOption("music", "Search lyrics for a music or for the music currently playing", false,
                ApplicationCommandOptionType.STRING);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = LyricsCmd.getSearch(context);
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading lyrics...", context.getAuthorName())
                .flatMap(messageId -> LyricsCmd.getMusixmatch(search)
                        .flatMap(musixmatch -> context.editFollowupMessage(messageId,
                                LyricsCmd.formatEmbed(musixmatch, context.getAuthorAvatar())))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No lyrics found matching `%s`",
                                context.getAuthorName(), search)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final Musixmatch musixmatch, final String avatarUrl) {
        final String artist = StringUtil.abbreviate(musixmatch.getArtist(), MAX_TITLE_LENGTH);
        final String title = StringUtil.abbreviate(musixmatch.getTitle(), MAX_TITLE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor("Lyrics: %s - %s".formatted(artist, title), musixmatch.getUrl(), avatarUrl)
                        .setThumbnail(musixmatch.getImageUrl())
                        .setDescription(StringUtil.abbreviate(musixmatch.getLyrics(), MAX_LYRICS_LENGTH))
                        .setFooter("Click on the title to see the full version",
                                "https://i.imgur.com/G7q6Hmq.png"));
    }

    private static Mono<Musixmatch> getMusixmatch(String search) {
        return LyricsCmd.getCorrectedUrl(search)
                .flatMap(url -> LyricsCmd.getLyricsDocument(url)
                        .map(doc -> doc.outputSettings(PRESERVE_FORMAT))
                        .map(doc -> new Musixmatch(doc, url)))
                .filter(musixmatch -> !musixmatch.getLyrics().isBlank());
    }

    /**
     * @return The search term, either the current playing music title or the context argument.
     */
    private static String getSearch(Context context) {
        return context.getOptionAsString("music")
                .orElseGet(() -> {
                    final GuildMusic guildMusic = MusicManager.getInstance()
                            .getGuildMusic(context.getGuildId())
                            .orElseThrow(NO_TRACK_EXCEPTION);

                    final AudioTrack track = guildMusic.getTrackScheduler().getAudioPlayer().getPlayingTrack();
                    if (track == null) {
                        throw NO_TRACK_EXCEPTION.get();
                    }
                    final AudioTrackInfo info = track.getInfo();
                    // Remove from title (case insensitive): official, video, music, [, ], (, )
                    return PATTERN.matcher(info.title).replaceAll("").trim();
                });
    }

    private static Mono<Document> getLyricsDocument(String url) {
        // Sometimes Musixmatch redirects to a wrong page
        // If the response URL and the requested URL are different, retry
        return RequestHelper.fromUrl(url)
                .request()
                .responseSingle((res, con) -> con.asString(StandardCharsets.UTF_8)
                        .map(body -> Tuples.of(res, body)))
                .timeout(Config.TIMEOUT)
                .flatMap(TupleUtils.function((res, body) -> {
                    if (url.endsWith(res.uri())) {
                        return Mono.just(Jsoup.parse(body));
                    }
                    return Mono.error(new ServerAccessException(res, body));
                }))
                .retryWhen(Retry.max(MAX_RETRY)
                        .filter(ServerAccessException.class::isInstance))
                .onErrorMap(Exceptions::isRetryExhausted,
                        err -> new IOException("Musixmatch does not redirect to the correct page: %s".formatted(url)));
    }

    private static Mono<String> getCorrectedUrl(String query) {
        final String url = String.format("%s/search/%s/tracks", HOME_URL, NetUtil.encode(query));
        // Make a search request on the site
        return RequestHelper.request(url)
                .map(Jsoup::parse)
                .map(doc -> doc.getElementsByClass("media-card-title"))
                .filter(Predicate.not(Elements::isEmpty))
                .map(Elements::first)
                // Find the first element containing "title" (generally the best result) and get its URL
                .map(trackList -> HOME_URL + trackList.getElementsByClass("title").attr("href"));
    }

}
