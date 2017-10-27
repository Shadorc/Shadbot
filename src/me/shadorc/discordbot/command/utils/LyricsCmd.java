package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.jsoup.nodes.Document;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class LyricsCmd extends AbstractCommand {

	private static final String HOME_URL = "https://www.musixmatch.com";
	private static final int MAX_LYRICS_LENGTH = EmbedBuilder.DESCRIPTION_CONTENT_LIMIT / 4;

	private final RateLimiter rateLimiter;

	public LyricsCmd() {
		super(CommandCategory.UTILS, Role.USER, "lyrics");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		String[] args = context.getArg().replaceAll("<|>", "").split("-", 2);
		if(args.length != 2) {
			throw new MissingArgumentException();
		}

		String artistSrch = args[0].trim();
		if(artistSrch.isEmpty()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You must specify an artist.", context.getChannel());
			return;
		}

		String titleSrch = args[1].trim();
		if(titleSrch.isEmpty()) {
			BotUtils.sendMessage(Emoji.GREY_EXCLAMATION + " You must specify a title.", context.getChannel());
			return;
		}

		try {
			artistSrch = URLEncoder.encode(artistSrch.replaceAll("[^A-Za-z]", "-"), "UTF-8");
			titleSrch = URLEncoder.encode(titleSrch.replaceAll("[^A-Za-z]", "-"), "UTF-8");

			String url = HOME_URL + "/lyrics/" + artistSrch + "/" + titleSrch;

			if(NetUtils.getResponse(url).statusCode() == 404) {
				Document searchDoc = NetUtils.getDoc(HOME_URL + "/search/" + artistSrch + "-" + titleSrch + "/tracks");
				if(!searchDoc.getElementsByClass("empty").isEmpty()) {
					BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No lyrics found for \"" + context.getArg() + "\"", context.getChannel());
					return;
				}
				url = HOME_URL + searchDoc.getElementsByClass("title").attr("href");
			}

			// makes html() preserve linebreaks and spacing
			Document doc = NetUtils.getDoc(url).outputSettings(new Document.OutputSettings().prettyPrint(false));

			String artist = doc.getElementsByClass("mxm-track-title__artist").html();
			String title = doc.getElementsByClass("mxm-track-title__track ").text().replace("Lyrics", "");
			String albumImg = "https:" + doc.getElementsByClass("banner-album-image").select("img").first().attr("src");
			String lyrics = doc.getElementsByClass("mxm-lyrics__content ").html();
			if(lyrics.length() > MAX_LYRICS_LENGTH) {
				lyrics = lyrics.substring(0, lyrics.substring(MAX_LYRICS_LENGTH).indexOf("\n") + MAX_LYRICS_LENGTH) + "...";
			}

			EmbedBuilder embed = Utils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Lyrics (" + artist + " - " + title + ")")
					.withUrl(url)
					.withThumbnail(albumImg)
					.appendDescription(url + "\n\n" + lyrics);
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (IOException err) {
			ExceptionUtils.manageException("getting lyrics", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show lyrics for a song.**")
				.appendField("Usage", "`" + context.getPrefix() + "lyrics <artist> - <title>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
