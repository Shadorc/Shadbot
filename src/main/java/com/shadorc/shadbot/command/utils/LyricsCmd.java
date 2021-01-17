package com.shadorc.shadbot.command.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.api.ServerAccessException;
import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.RequestHelper;
import com.shadorc.shadbot.utils.NetUtils;
import com.shadorc.shadbot.utils.ShadbotUtils;
import com.shadorc.shadbot.utils.StringUtils;
import discord4j.core.object.Embed;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.discordjson.json.ImmutableApplicationCommandRequest;
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
import java.util.regex.Pattern;

public class LyricsCmd extends BaseCmd {

    private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;
    private static final int MAX_TITLE_LENGTH = Embed.MAX_TITLE_LENGTH / 3;

    // Make html() preserve linebreak and spacing
    private static final OutputSettings PRESERVE_FORMAT = new OutputSettings().prettyPrint(false);
    private static final String HOME_URL = "https://www.musixmatch.com";
    private static final int MAX_RETRY = 5;
    private static final Pattern PATTERN = Pattern.compile("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)");

    public LyricsCmd() {
        super(CommandCategory.UTILS, "lyrics", "Show lyrics for a song or for the current music");
        this.setDefaultRateLimiter();
    }

    @Override
    public ApplicationCommandRequest build(ImmutableApplicationCommandRequest.Builder builder) {
        return builder
                .addOption(ApplicationCommandOptionData.builder()
                        .name("music")
                        .description("Music's name to search")
                        .type(ApplicationCommandOptionType.STRING.getValue())
                        .required(false)
                        .build())
                .build();
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = LyricsCmd.getSearch(context);
        return context.createFollowupMessage(Emoji.HOURGLASS + " (**%s**) Loading lyrics...", context.getAuthorName())
                .flatMap(messageId -> LyricsCmd.getMusixmatch(search)
                        .flatMap(musixmatch -> context.editFollowupMessage(messageId,
                                LyricsCmd.formatEmbed(musixmatch, context.getAuthorAvatarUrl())))
                        .switchIfEmpty(context.editFollowupMessage(messageId,
                                Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
                                context.getAuthorName(), search)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(final Musixmatch musixmatch, final String avatarUrl) {
        final String artist = StringUtils.abbreviate(musixmatch.getArtist(), MAX_TITLE_LENGTH);
        final String title = StringUtils.abbreviate(musixmatch.getTitle(), MAX_TITLE_LENGTH);
        return ShadbotUtils.getDefaultEmbed(
                embed -> embed.setAuthor(String.format("Lyrics: %s - %s", artist, title),
                        musixmatch.getUrl(), avatarUrl)
                        .setThumbnail(musixmatch.getImageUrl())
                        .setDescription(StringUtils.abbreviate(musixmatch.getLyrics(), MAX_LYRICS_LENGTH))
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

    /*
     * @return The search term, either the current playing music title or the context argument.
     */
    private static String getSearch(Context context) {
        return context.getOption("music")
                .orElseGet(() -> {
                    final GuildMusic guildMusic = MusicManager.getInstance()
                            .getGuildMusic(context.getGuildId())
                            .orElseThrow(MissingArgumentException::new);

                    final AudioTrack track = guildMusic.getTrackScheduler().getAudioPlayer().getPlayingTrack();
                    if (track == null) {
                        throw new MissingArgumentException();
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
                        err -> new IOException(String.format("Musixmatch does not redirect to the correct page: %s", url)));
    }

    private static Mono<String> getCorrectedUrl(String search) {
        final String url = String.format("%s/search/%s/tracks", HOME_URL, NetUtils.encode(search));
        // Make a search request on the site
        return RequestHelper.request(url)
                .map(Jsoup::parse)
                .map(doc -> doc.getElementsByClass("media-card-title"))
                .filter(elements -> !elements.isEmpty())
                .map(Elements::first)
                // Find the first element containing "title" (generally the best result) and get its URL
                .map(trackList -> HOME_URL + trackList.getElementsByClass("title").attr("href"));
    }

}
