package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.http.HttpStatus;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import discord4j.core.DiscordClient;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.musixmatch.Musixmatch;
import me.shadorc.shadbot.core.command.BaseCmd;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.music.GuildMusic;
import me.shadorc.shadbot.music.GuildMusicManager;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.help.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

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
		final Optional<String> arg = context.getArg();

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String search;
			if(arg.isPresent()) {
				search = arg.get();
			} else {
				final GuildMusic guildMusic = GuildMusicManager.GUILD_MUSIC_MAP.get(context.getGuildId());
				if(guildMusic == null) {
					loadingMsg.stopTyping();
					throw new MissingArgumentException();
				}

				final AudioTrackInfo info = guildMusic.getTrackScheduler().getAudioPlayer().getPlayingTrack().getInfo();
				// Remove from title (case insensitive): official, video, music, [, ], (, )
				search = info.title.replaceAll("(?i)official|video|music|\\[|\\]|\\(|\\)", "");
			}

			final String url = this.getCorrectedUrl(search);
			if(url == null) {
				return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
						context.getUsername(), search))
						.then();
			}

			final Document doc = this.getLyricsDocument(context.getClient(), url).outputSettings(PRESERVE_FORMAT);
			final Musixmatch musixmatch = new Musixmatch(doc);

			final Consumer<EmbedCreateSpec> embedConsumer = EmbedUtils.getDefaultEmbed()
					.andThen(embed -> embed.setAuthor(String.format("Lyrics: %s - %s",
							musixmatch.getArtist(), musixmatch.getTitle()), url, context.getAvatarUrl())
							.setThumbnail(musixmatch.getImageUrl())
							.setDescription(musixmatch.getLyrics())
							.setFooter("Click on the title to see the full version",
									"https://www.shareicon.net/download/2015/09/11/99440_info_512x512.png"));

			return loadingMsg.send(embedConsumer).then();

		} catch (final Exception err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private Document getLyricsDocument(DiscordClient client, String url) throws IOException {
		// Sometimes Musixmatch redirects to a wrong page
		// If the response URL and the requested URL are different, retry
		int retryCount = 0;
		Response response = null;
		do {
			if(retryCount == MAX_RETRY) {
				LogUtils.warn(client, String.format("[%s] Too many retries, abort attempt to reload page.",
						this.getClass().getSimpleName()));
				throw new HttpStatusException("Musixmatch does not redirect to the correct page.", HttpStatus.SC_SERVICE_UNAVAILABLE, url);
			}

			response = NetUtils.getResponse(url);
			retryCount++;
		} while(!response.url().toString().equalsIgnoreCase(url));

		return response.parse();
	}

	private String getCorrectedUrl(String search) throws IOException {
		final String url = String.format("%s/search/%s/tracks", HOME_URL, NetUtils.encode(search));

		// Make a search request on the site
		final Document doc = NetUtils.getDoc(url);
		final Element trackList = doc.getElementsByClass("media-card-title").first();
		if(trackList == null) {
			return null;
		}

		// Find the first element containing "title" (generally the best result) and get its URL
		return HOME_URL + trackList.getElementsByClass("title").attr("href");
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
