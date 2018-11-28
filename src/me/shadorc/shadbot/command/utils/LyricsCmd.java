package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpStatus;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;

import discord4j.core.DiscordClient;
import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.api.musixmatch.Musixmatch;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.object.Emoji;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "lyrics" })
public class LyricsCmd extends AbstractCommand {

	// Make html() preserve linebreaks and spacing
	private static final OutputSettings PRESERVE_FORMAT = new Document.OutputSettings().prettyPrint(false);
	private static final String HOME_URL = "https://www.musixmatch.com";
	private static final int MAX_RETRY = 5;

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2, "-");

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final String artist = NetUtils.encode(args.get(0).replace(" ", "-"));
			final String title = NetUtils.encode(args.get(1).replace(" ", "-"));

			// Make a direct search with the artist and the title
			String url = String.format("%s/lyrics/%s/%s", HOME_URL, artist, title);
			Response response = this.getLyricsResponse(context.getClient(), url);

			// If the direct search found nothing
			if(response.statusCode() == HttpStatus.SC_NOT_FOUND || response.body().contains("Oops! We couldn't find that page.")) {
				url = this.getCorrectedUrl(artist, title);
				if(url == null) {
					return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
							context.getUsername(), context.getArg().get()))
							.then();
				}
				response = this.getLyricsResponse(context.getClient(), url);
			}

			final Document doc = response.parse().outputSettings(PRESERVE_FORMAT);
			final Musixmatch musixmatch = new Musixmatch(doc);
			final String finalUrl = url;

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Lyrics: %s - %s",
									musixmatch.getArtist(), musixmatch.getTitle()), finalUrl, avatarUrl)
							.setThumbnail(musixmatch.getImageUrl())
							.setDescription(musixmatch.getLyrics())
							.setFooter("Click on the title to see the full version", "https://www.shareicon.net/download/2015/09/11/99440_info_512x512.png"))
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	private Response getLyricsResponse(DiscordClient client, String url) throws IOException {
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

		return response;
	}

	private String getCorrectedUrl(String artist, String title) throws IOException {
		final String url = String.format("%s/search/%s-%s", HOME_URL, artist, title);

		// Make a search request on the site
		final Document doc = NetUtils.getDoc(url);
		final Element trackList = doc.getElementsByClass("tracks list").first();
		if(trackList == null) {
			return null;
		}

		// Find the first element containing "title" (generally the best result) and get its URL
		return HOME_URL + trackList.getElementsByClass("title").attr("href");
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show lyrics for a song.")
				.setDelimiter(" - ")
				.addArg("artist", false)
				.addArg("title", false)
				.setSource(HOME_URL)
				.build();
	}
}
