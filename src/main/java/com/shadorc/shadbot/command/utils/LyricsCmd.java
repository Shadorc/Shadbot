package com.shadorc.shadbot.command.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.api.html.musixmatch.Musixmatch;
import com.shadorc.shadbot.command.MissingArgumentException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.music.GuildMusic;
import com.shadorc.shadbot.music.MusicManager;
import com.shadorc.shadbot.object.Emoji;
import com.shadorc.shadbot.object.help.HelpBuilder;
import com.shadorc.shadbot.object.message.UpdatableMessage;
import com.shadorc.shadbot.utils.DiscordUtils;
import com.shadorc.shadbot.utils.LogUtils;
import com.shadorc.shadbot.utils.NetUtils;
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
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LyricsCmd extends BaseCmd {

    // Make html() preserve linebreak and spacing
    private static final OutputSettings PRESERVE_FORMAT = new OutputSettings().prettyPrint(false);
    private static final String HOME_URL = "https://www.musixmatch.com";
    private static final int MAX_RETRY = 5;
    private static final Pattern PATTERN = Pattern.compile("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)");

    public LyricsCmd() {
        super(CommandCategory.UTILS, List.of("lyrics"));
        this.setDefaultRateLimiter();
    }

    @Override
    public Mono<Void> execute(Context context) {
        final UpdatableMessage updatableMsg = new UpdatableMessage(context.getClient(), context.getChannelId());
        final String search = this.getSearch(context);

        return updatableMsg.setContent(String.format(Emoji.HOURGLASS + " (**%s**) Loading lyrics...",
                context.getUsername()))
                .send()
                .then(this.getMusixmatch(search))
                .map(musixmatch -> updatableMsg.setEmbed(DiscordUtils.getDefaultEmbed()
                        .andThen(embed -> embed.setAuthor(String.format("Lyrics: %s - %s",
                                musixmatch.getArtist(), musixmatch.getTitle()), musixmatch.getUrl(), context.getAvatarUrl())
                                .setThumbnail(musixmatch.getImageUrl())
                                .setDescription(musixmatch.getLyrics())
                                .setFooter("Click on the title to see the full version",
                                        "https://i.imgur.com/G7q6Hmq.png"))))
                .switchIfEmpty(Mono.defer(() -> Mono.just(updatableMsg.setContent(
                        String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
                                context.getUsername(), search)))))
                .flatMap(UpdatableMessage::send)
                .onErrorResume(err -> updatableMsg.deleteMessage().then(Mono.error(err)))
                .then();
    }

    private Mono<Musixmatch> getMusixmatch(String search) {
        return this.getCorrectedUrl(search)
                .flatMap(url -> this.getLyricsDocument(url)
                        .map(doc -> doc.outputSettings(PRESERVE_FORMAT))
                        .map(doc -> new Musixmatch(doc, url)))
                .filter(musixmatch -> !musixmatch.getLyrics().isBlank());
    }

    /**
     * @return The search term, either the current playing music title or the context argument.
     */
    private String getSearch(Context context) {
        return context.getArg()
                .orElseGet(() -> {
                    final GuildMusic guildMusic = MusicManager.getInstance()
                            .getMusic(context.getGuildId())
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

    private Mono<Document> getLyricsDocument(String url) {
        // Sometimes Musixmatch redirects to a wrong page
        // If the response URL and the requested URL are different, retry
        return NetUtils.request(HttpMethod.GET, url)
                .responseSingle((res, con) -> con.asString(StandardCharsets.UTF_8)
                        .map(body -> Tuples.of(res, body)))
                .timeout(Config.TIMEOUT)
                .flatMap(responseSingle -> {
                    if (url.endsWith(responseSingle.getT1().uri())) {
                        return Mono.just(Jsoup.parse(responseSingle.getT2()));
                    }
                    return Mono.error(new IOException("Musixmatch redirected to wrong page."));
                })
                .retryWhen(Retry.max(MAX_RETRY)
                        .filter(err -> "Musixmatch redirected to wrong page.".equals(err.getMessage())))
                .onErrorMap(IOException.class, err -> {
                    LogUtils.warn("[%s] Too many retries, abort attempt to reload page.", this.getClass().getSimpleName());
                    return new HttpStatusException("Musixmatch does not redirect to the correct page.",
                            HttpStatus.SC_SERVICE_UNAVAILABLE, url);
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
        return HelpBuilder.create(this, context)
                .setDescription("Show lyrics for a song."
                        + "\nCan also be used without argument when a music is being played to find corresponding lyrics.")
                .addArg("search", true)
                .setSource(HOME_URL)
                .build();
    }
}
