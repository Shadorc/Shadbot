package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class ImageCmd extends Command {

	private static final String API_URL = "https://www.deviantart.com/api/v1/oauth2/";

	private final RateLimiter rateLimiter;
	private String deviantArtToken;

	public ImageCmd() {
		super(false, "image");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			BotUtils.sendMessage(Emoji.INFO + " " + context.getAuthorName() + ", you have to wait "
					+ String.format("%.1f", rateLimiter.getRemainingTime(context.getGuild(), context.getAuthor()))
					+ " second(s) before using this command again.", context.getChannel());
			return;
		}

		try {
			if(this.deviantArtToken == null) {
				this.generateAccessToken();
			}

			String encodedSearch = URLEncoder.encode(context.getArg(), "UTF-8");
			// String encodedSearch = URLEncoder.encode(context.getArg().replace(" ", ""), "UTF-8");

			JSONObject resultObj = this.getRandomPopularResult(encodedSearch);
			// JSONObject resultObj = this.getRandomResult(encodedSearch);

			// if(resultObj == null) {
			// encodedSearch = this.getTag(encodedSearch);
			// if(encodedSearch == null) {
			// BotUtils.sendMessage(Emoji.WARNING + " No result found for \"" + context.getArg() + "\"", context.getChannel());
			// return;
			// }
			// resultObj = this.getRandomResult(encodedSearch);
			// }

			if(resultObj == null) {
				BotUtils.sendMessage(Emoji.WARNING + " No results for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			JSONObject authorObj = resultObj.getJSONObject("author");
			JSONObject contentObj = resultObj.getJSONObject("content");

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("DeviantArt Search (" + encodedSearch + ")")
					.withAuthorIcon(context.getGuild().getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
					.withColor(Config.BOT_COLOR)
					.appendField("Author", authorObj.getString("username"), false)
					.appendField("Title", resultObj.getString("title"), false)
					.appendField("Category", resultObj.getString("category_path"), false)
					.appendField("URL", resultObj.getString("url"), false)
					.withImage(contentObj.getString("src"));

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (SocketTimeoutException sterr) {
			BotUtils.sendMessage(Emoji.WARNING + " DeviantArt's servers are busy, please try again later.", context.getChannel());
		} catch (IOException e) {
			Log.error("An error occured while getting image.", e, context.getChannel());
		}
	}

	private void generateAccessToken() throws SocketTimeoutException {
		this.deviantArtToken = null;

		try {
			String oauthStr = HtmlUtils.getHTML(new URL("https://www.deviantart.com/oauth2/token?"
					+ "client_id=" + Storage.getApiKey(ApiKeys.DEVIANTART_CLIENT_ID)
					+ "&client_secret=" + Storage.getApiKey(ApiKeys.DEVIANTART_API_SECRET)
					+ "&grant_type=client_credentials"));
			JSONObject oauthObj = new JSONObject(oauthStr);
			this.deviantArtToken = oauthObj.getString("access_token");

		} catch (SocketTimeoutException sterr) {
			throw sterr;
		} catch (IOException e) {
			Log.error("Error while getting DeviantArt Access Token.", e);
		}
	}

	/*
	 * private JSONObject getRandomResult(String encodedSearch) throws IOException { JSONObject mainObj = new JSONObject(HtmlUtils.getHTML(new
	 * URL(API_URL+ "browse/tags?" + "tag=" + encodedSearch + "&limit=1" //The pagination limit (min: 1 max: 50) + "&offset=" + MathUtils.rand(5000) //The
	 * pagination offset (min: 0 max: 50000) + "&access_token=" + this.deviantArtToken))); JSONArray resultsArray = mainObj.getJSONArray("results");
	 * if(resultsArray.length() == 0) { return null; } return resultsArray.getJSONObject(MathUtils.rand(resultsArray.length())); }
	 */

	private JSONObject getRandomPopularResult(String encodedSearch) throws IOException {
		JSONObject mainObj = new JSONObject(HtmlUtils.getHTML(new URL(API_URL + "browse/popular?"
				+ "q=" + encodedSearch
				+ "&timerange=alltime"
				+ "&limit=1" // The pagination limit (min: 1 max: 50)
				+ "&offset=" + MathUtils.rand(150) // The pagination offset (min: 0 max: 50000)
				+ "&access_token=" + this.deviantArtToken)));
		JSONArray resultsArray = mainObj.getJSONArray("results");

		if(resultsArray.length() == 0) {
			return null;
		}

		return resultsArray.getJSONObject(MathUtils.rand(resultsArray.length()));
	}

	/*
	 * private String getTag(String encodedSearch) throws IOException { JSONObject mainObj = new JSONObject(HtmlUtils.getHTML(new URL(API_URL +
	 * "browse/tags/search?" + "tag_name=" + encodedSearch + "&access_token=" + deviantArtToken))); JSONArray resultsArray =
	 * mainObj.getJSONArray("results"); if(resultsArray.length() == 0) { return null; } return resultsArray.getJSONObject(0).getString("tag_name"); }
	 */

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Search for a random image on DeviantArt.**")
				.appendField("Usage", context.getPrefix() + "image <search>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
