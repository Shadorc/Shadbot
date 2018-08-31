package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jsoup.Connection.Response;
import org.jsoup.HttpStatusException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.command.Emoji;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.embed.log.LogUtils;
import me.shadorc.shadbot.utils.message.LoadingMessage;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "lyrics" })
public class LyricsCmd extends AbstractCommand {

	// Make html() preserve linebreaks and spacing
	private static final OutputSettings PRESERVE_FORMAT = new Document.OutputSettings().prettyPrint(false);
	private static final String HOME_URL = "https://www.musixmatch.com";
	private static final int MAX_LYRICS_LENGTH = DiscordUtils.DESCRIPTION_CONTENT_LIMIT / 2;

	@Override
	public Mono<Void> execute(Context context) {
		final List<String> args = context.requireArgs(2, "-");

		final LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			final String artistSrch = NetUtils.encode(args.get(0).replaceAll("[^A-Za-z0-9]", "-"));
			final String titleSrch = NetUtils.encode(args.get(1).replaceAll("[^A-Za-z0-9]", "-"));

			// Make a direct search with the artist and the title
			String url = String.format("%s/lyrics/%s/%s", HOME_URL, artistSrch, titleSrch);

			// Sometimes musixmatch redirects to a wrong page, check it and reload it if necessary
			int retryCount = 0;
			Response response = null;
			do {
				if(response != null) {
					retryCount++;
					if(retryCount == 3) {
						LogUtils.warn(context.getClient(), String.format("[%s] %d retries, abort attempt to reload page.",
								this.getClass().getSimpleName(), retryCount));
						throw new HttpStatusException("musixmatch does not redirect to the correct page.", HttpStatus.SC_SERVICE_UNAVAILABLE, url);
					}
					LogUtils.infof("[%s] URLs do not match (%s / %s), reloading page.",
							this.getClass().getSimpleName(), response.url().toString(), url);
				}
				response = NetUtils.getResponse(url);
			} while(!response.url().toString().equalsIgnoreCase(url));

			Document doc = response.parse().outputSettings(PRESERVE_FORMAT);

			// If the direct search found nothing
			if(response.statusCode() == HttpStatus.SC_NOT_FOUND || doc.text().contains("Oops! We couldn't find that page.")) {

				final String searchUrl = String.format("%s/search/%s-%s?", HOME_URL, artistSrch, titleSrch);

				// Make a search request on the site
				final Document searchDoc = NetUtils.getDoc(searchUrl);
				final Element trackListElement = searchDoc.getElementsByClass("tracks list").first();
				if(trackListElement == null) {
					return loadingMsg.send(String.format(Emoji.MAGNIFYING_GLASS + " (**%s**) No Lyrics found for `%s`",
							context.getUsername(), context.getArg().get()))
							.then();
				}

				// Find the first element containing "title" (generally the best result) and get its URL
				url = HOME_URL + trackListElement.getElementsByClass("title").attr("href");
				doc = NetUtils.getDoc(url).outputSettings(PRESERVE_FORMAT);
			}

			final String artist = doc.getElementsByClass("mxm-track-title__artist").html();
			final String title = StringUtils.remove(doc.getElementsByClass("mxm-track-title__track ").text(), "Lyrics");
			final String albumImg = "https:" + doc.getElementsByClass("banner-album-image").select("img").first().attr("src");
			final String lyrics = StringUtils.abbreviate(doc.getElementsByClass("mxm-lyrics__content ").html(), MAX_LYRICS_LENGTH);
			final String finalUrl = url;

			return context.getAvatarUrl()
					.map(avatarUrl -> EmbedUtils.getDefaultEmbed()
							.setAuthor(String.format("Lyrics (%s - %s)", artist, title), finalUrl, avatarUrl)
							.setThumbnail(albumImg)
							.setDescription(lyrics)
							.setFooter("Click on the title to see the full version", "https://www.shareicon.net/download/2015/09/11/99440_info_512x512.png"))
					.flatMap(loadingMsg::send)
					.then();

		} catch (IOException err) {
			loadingMsg.stopTyping();
			throw Exceptions.propagate(err);
		}
	}

	@Override
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show lyrics for a song.")
				.setDelimiter(" - ")
				.setSource(HOME_URL)
				.build();
	}
}
