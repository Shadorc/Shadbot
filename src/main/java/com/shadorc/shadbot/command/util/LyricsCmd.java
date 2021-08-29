package com.shadorc.shadbot.command.util;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.Cmd;
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
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec;
import discord4j.rest.util.ApplicationCommandOptionType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.select.Elements;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class LyricsCmd extends Cmd {

    private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;
    private static final int MAX_TITLE_LENGTH = Embed.MAX_TITLE_LENGTH / 3;
    private static final int MAX_RETRY = 5;

    private static final String HOME_URL = "https://www.musixmatch.com";
    // Make html() preserve linebreak and spacing
    private static final OutputSettings PRESERVE_FORMAT = new OutputSettings().prettyPrint(false);
    private static final Pattern PATTERN = Pattern.compile("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)");

    public LyricsCmd() {
        super(CommandCategory.UTILS, "lyrics", "Search for music lyrics");
        this.addOption(option -> option.name("music")
                .description("Search lyrics for a music or for the music currently playing")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue()));
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = LyricsCmd.getSearch(context);
        return context.createFollowupMessage(Emoji.HOURGLASS, context.localize("lyrics.loading"))
                .then(LyricsCmd.getMusixmatch(search))
                .flatMap(musixmatch -> context.editFollowupMessage(LyricsCmd.formatEmbed(context, musixmatch)))
                .switchIfEmpty(context.editFollowupMessage(Emoji.MAGNIFYING_GLASS,
                        context.localize("lyrics.not.found").formatted(search)));
    }

    private static Consumer<LegacyEmbedCreateSpec> formatEmbed(Context context, Musixmatch musixmatch) {
        final String artist = StringUtil.abbreviate(musixmatch.getArtist(), MAX_TITLE_LENGTH);
        final String musicTitle = StringUtil.abbreviate(musixmatch.getTitle(), MAX_TITLE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("lyrics.title").formatted(artist, musicTitle),
                                musixmatch.url(), context.getAuthorAvatar())
                        .setThumbnail(musixmatch.getImageUrl())
                        .setDescription(StringUtil.abbreviate(musixmatch.getLyrics(), MAX_LYRICS_LENGTH))
                        .setFooter(context.localize("lyrics.footer"), "https://i.imgur.com/G7q6Hmq.png"));
    }

    private static Mono<Musixmatch> getMusixmatch(String search) {
        return LyricsCmd.getCorrectedUrl(search)
                .flatMap(url -> LyricsCmd.getLyricsDocument(url)
                        .map(doc -> doc.outputSettings(PRESERVE_FORMAT))
                        .map(doc -> new Musixmatch(doc, url)))
                .filter(musixmatch -> !musixmatch.getLyrics().isBlank());
    }

    /*
     * @return The search term, either the current playing music title or the context argument.
     */
    private static String getSearch(Context context) {
        return context.getOptionAsString("music")
                .orElseGet(() -> {
                    final GuildMusic guildMusic = MusicManager.getGuildMusic(context.getGuildId())
                            .orElseThrow(() -> new CommandException(context.localize("lyrics.not.listening")));

                    final AudioTrack track = guildMusic.getTrackScheduler().getAudioPlayer().getPlayingTrack();
                    if (track == null) {
                        throw new CommandException(context.localize("lyrics.not.listening"));
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
                        .filter(ServerAccessException.class::isInstance)
                        .onRetryExhaustedThrow((spec, signal) ->
                                new IOException("Musixmatch does not redirect to the correct page: %s".formatted(url))));
    }

    private static Mono<String> getCorrectedUrl(String query) {
        final String url = "%s/search/%s/tracks".formatted(HOME_URL, NetUtil.encode(query));
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
