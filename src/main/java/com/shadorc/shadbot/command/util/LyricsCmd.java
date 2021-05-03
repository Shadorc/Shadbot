package com.shadorc.shadbot.command.util;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.api.html.Genius.Lyrics;
import com.shadorc.shadbot.api.json.genius.GeniusResponse;
import com.shadorc.shadbot.api.json.genius.Hit;
import com.shadorc.shadbot.api.json.genius.Response;
import com.shadorc.shadbot.api.json.genius.Result;
import com.shadorc.shadbot.command.CommandException;
import com.shadorc.shadbot.core.command.BaseCmd;
import com.shadorc.shadbot.core.command.CommandCategory;
import com.shadorc.shadbot.core.command.Context;
import com.shadorc.shadbot.data.credential.Credential;
import com.shadorc.shadbot.data.credential.CredentialManager;
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
import io.netty.handler.codec.http.HttpHeaderNames;
import org.jsoup.Jsoup;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class LyricsCmd extends BaseCmd {

    private static final int MAX_LYRICS_LENGTH = Embed.MAX_DESCRIPTION_LENGTH / 3;
    private static final int MAX_TITLE_LENGTH = Embed.MAX_TITLE_LENGTH / 3;
    private static final Pattern PATTERN = Pattern.compile("(?i)official|officiel|clip|video|music|\\[|]|\\(|\\)");
    private static final String HOME_URL = "https://api.genius.com";
    private static final String SEARCH_URl = "%s/search".formatted(HOME_URL);

    private final String accessToken;

    public LyricsCmd() {
        super(CommandCategory.UTILS, "lyrics", "Search for music lyrics");
        this.addOption(option -> option.name("music")
                .description("Search lyrics for a music or for the music currently playing")
                .required(false)
                .type(ApplicationCommandOptionType.STRING.getValue()));

        this.accessToken = CredentialManager.get(Credential.GENIUS_ACCESS_TOKEN);
    }

    @Override
    public Mono<?> execute(Context context) {
        final String search = LyricsCmd.getSearch(context);
        return context.reply(Emoji.HOURGLASS, context.localize("lyrics.loading"))
                .then(this.getHit(search)
                        .flatMap(result -> Mono.zip(
                                Mono.just(result),
                                RequestHelper.request(result.url())
                                        .map(Jsoup::parse)
                                        .map(Lyrics::new))))
                .flatMap(TupleUtils.function((result, lyrics) ->
                        context.editReply(LyricsCmd.formatEmbed(context, result, lyrics))))
                .switchIfEmpty(context.editReply(Emoji.MAGNIFYING_GLASS,
                        context.localize("lyrics.not.found").formatted(search)));
    }

    private static Consumer<EmbedCreateSpec> formatEmbed(Context context, Result result, Lyrics lyrics) {
        final String title = StringUtil.abbreviate(result.fullTitle(), MAX_TITLE_LENGTH);
        return ShadbotUtil.getDefaultEmbed(
                embed -> embed.setAuthor(context.localize("lyrics.title").formatted(title),
                        result.url(), context.getAuthorAvatar())
                        .setThumbnail(result.thumbnail())
                        .setDescription(StringUtil.abbreviate(lyrics.getText(), MAX_LYRICS_LENGTH))
                        .setFooter(context.localize("lyrics.footer"), "https://i.imgur.com/G7q6Hmq.png"));
    }

    private Mono<Result> getHit(String search) {
        return RequestHelper.fromUrl("%s?q=%s".formatted(SEARCH_URl, NetUtil.encode(search)))
                .addHeaders(HttpHeaderNames.AUTHORIZATION, "Bearer %s".formatted(this.accessToken))
                .to(GeniusResponse.class)
                .map(GeniusResponse::response)
                .flatMapIterable(Response::hits)
                .next()
                .map(Hit::result);
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

}
