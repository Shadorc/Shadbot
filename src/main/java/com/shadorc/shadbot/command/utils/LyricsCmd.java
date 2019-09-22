package com.shadorc.shadbot.command.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.Config;
import com.shadorc.shadbot.api.musixmatch.Musixmatch;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.exception.MissingArgumentException;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.LoadingMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NetUtils;
import discord4j.core.DiscordClient;
import discord4j.core.spec.EmbedCreateSpec;
import io.netty.handler.codec.http.HttpMethod;
import org.apache.http.HttpStatus;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.select.Elements;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

public class LyricsCmd extends BaseCmd {

    // Make html() preserve linebreaks and spacing
    private static final OutputSettings PRESERVE_FORMAT = new Document.OutputSettings().prettyPrint(false);
    private static final String HOME_URL = "https://www.musixmatch.com";
    private static final int MAX_RETRY = 5;

    public LyricsCmd() {
        super(CommandCategory.UTILS, List.of("lyrics"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());
        final String search = this.getSearch(context);

        return this.getCorrectedUrl(search)
                .flatMap(url -> Mono.zip(this.getLyricsDocument(context.getClient(), url)
                        .map(doc -> doc.outputSettings(PRESERVE_FORMAT)), Mono.just(url)))
                .flatMap(tuple -> {
                    final Document doc = tuple.getT1();
                    final String url = tuple.getT2();

                    final Musixmatch musixmatch = new Musixmatch(doc);
                    if (musixmatch.getLyrics().isBlank()) {
                        return Mono.empty();
                    }

                    return Mono.just(loadingMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                            .andThen(embed -> embed.setAuthor(String.format("Lyrics: %s - %s",
                                    musixmatch.getArtist(), musixmatch.getTitle()), url, context.getAvatarUrl())
                                    .setThumbnail(musixmatch.getImageUrl())
                                    .setDescription(musixmatch.getLyrics())
                                    .setFooter("Click on the title to see the full version",
                                            "https://i.imgur.com/G7q6Hmq.png"))));
                })
                .switchIfEmpty(Mono.defer(() -> Mono.just(loadingMsg.setContent(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
                        context.getUsername(), search)))))
                .flatMap(LoadingMessage::send)
                .doOnTerminate(loadingMsg::stopTyping)
                .then();
    }

    private String getSearch(Context context) {
        return context.getArg()
                .orElseGet(() -> {
                    final GuildMusic guildMusic = MusicManager.getInstance().getMusic(context.getGuildId());
                    if (guildMusic == null) {
                        throw new MissingArgumentException();
                    }

                    final AudioTrack track = guildMusic.getTrackScheduler().getAudioPlayer().getPlayingTrack();
                    if (track == null) {
                        throw new MissingArgumentException();
                    }
                    final AudioTrackInfo info = track.getInfo();
                    // Remove from title (case insensitive): official, video, music, [, ], (, )
                    return info.title.replaceAll("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)", "").trim();
                });
    }

    private Mono<Document> getLyricsDocument(DiscordClient client, String url) {
        // Sometimes Musixmatch redirects to a wrong page
        // If the response URL and the requested URL are different, retry
        return NetUtils.request(HttpMethod.GET, url)
                .responseSingle((res, con) -> con.asString(StandardCharsets.UTF_8)
                        .map(body -> Tuples.of(res, body)))
                .timeout(Config.DEFAULT_TIMEOUT)
                .flatMap(responseSingle -> {
                    if (url.endsWith(responseSingle.getT1().uri())) {
                        return Mono.just(Jsoup.parse(responseSingle.getT2()));
                    }
                    return Mono.error(new IOException("Musixmatch redirected to wrong page."));
                })
                .retry(MAX_RETRY, err -> err.getMessage().equals("Musixmatch redirected to wrong page."))
                .onErrorMap(IOException.class, err -> {
                    LogUtils.warn(client, String.format("[%s] Too many retries, abort attempt to reload page.",
                            this.getClass().getSimpleName()));
                    return new HttpStatusException("Musixmatch does not redirect to the correct page.", HttpStatus.SC_SERVICE_UNAVAILABLE, url);
                });
    }

    private Mono<String> getCorrectedUrl(String search) {
        final String url = String.format("%s/search/%s/tracks", HOME_URL, NetUtils.encode(search));
        // Make a search request on the site
        return NetUtils.get(url)
                .map(Jsoup::parse)
                .map(doc -> doc.getElementsByClass("media-card-title"))
                .filter(elements -> !elements.isEmpty())
                .map(Elements::first)
                // Find the first element containing "title" (generally the best result) and get its URL
                .map(trackList -> HOME_URL + trackList.getElementsByClass("title").attr("href"));
    }

    @Override
    public Consumer<EmbedCreateSpec> getHelp(Context context) {
        return new HelpBuilder(this, context)
                .setDescription("Show lyrics for a song."
                        + "\nCan also be used without argument when a music is being played to find corresponding lyrics.")
                .addArg("search", true)
                .setSource(HOME_URL)
                .build();
    }
}
