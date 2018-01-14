package me.shadorc.shadbot.command.utils;

import java.io.IOException;
import java.util.List;

import org.jsoup.Connection.Response;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Document.OutputSettings;
import org.jsoup.nodes.Element;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "lyrics" })
public class LyricsCmd extends AbstractCommand {

	// Make html() preserve linebreaks and spacing
	private static final OutputSettings PRESERVE_FORMAT = new Document.OutputSettings().prettyPrint(false);
	private static final String HOME_URL = "https://www.musixmatch.com";
	private static final int MAX_LYRICS_LENGTH = EmbedBuilder.DESCRIPTION_CONTENT_LIMIT / 4;

	@Override
	public void execute(Context context) throws MissingArgumentException {
		List<String> args = StringUtils.split(context.getArg(), 2, "-");
		if(args.size() != 2) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading lyrics...", context.getChannel());
		loadingMsg.send();

		try {
			String artistSrch = NetUtils.encode(args.get(0).replaceAll("[^A-Za-z0-9]", "-"));
			String titleSrch = NetUtils.encode(args.get(1).replaceAll("[^A-Za-z0-9]", "-"));

			// Make a direct search with the artist and the title
			String url = String.format("%s/lyrics/%s/%s", HOME_URL, artistSrch, titleSrch);

			Response response = NetUtils.getResponse(url);
			Document doc = NetUtils.getResponse(url).parse().outputSettings(PRESERVE_FORMAT);

			// If the direct search found nothing
			if(response.statusCode() == 404 || response.parse().text().contains("Oops! We couldn't find that page.")) {
				url = String.format("%s/search/%s-%s?", HOME_URL, artistSrch, titleSrch);
				// Make a search request on the site
				Document searchDoc = NetUtils.getDoc(url);
				Element trackListElement = searchDoc.getElementsByClass("tracks list").first();
				if(trackListElement == null) {
					loadingMsg.edit(TextUtils.noResult(context.getArg()));
					return;
				}
				// Find the first element containing "title" (generally the best result) and get its URL
				url = HOME_URL + trackListElement.getElementsByClass("title").attr("href");
				doc = NetUtils.getDoc(url).outputSettings(PRESERVE_FORMAT);
			}

			String artist = doc.getElementsByClass("mxm-track-title__artist").html();
			String title = StringUtils.remove(doc.getElementsByClass("mxm-track-title__track ").text(), "Lyrics");
			String albumImg = "https:" + doc.getElementsByClass("banner-album-image").select("img").first().attr("src");
			String lyrics = StringUtils.truncate(doc.getElementsByClass("mxm-lyrics__content ").html(), MAX_LYRICS_LENGTH);

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName(String.format("Lyrics (%s - %s)", artist, title))
					.withUrl(url)
					.withThumbnail(albumImg)
					.appendDescription(url + "\n\n" + lyrics);
			loadingMsg.edit(embed.build());

		} catch (IOException err) {
			loadingMsg.delete();
			Utils.handle("getting lyrics", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show lyrics for a song.")
				.setUsage("<artist> - <title>")
				.setSource(HOME_URL)
				.build();
	}
}
