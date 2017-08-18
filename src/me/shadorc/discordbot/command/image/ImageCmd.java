package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import sx.blah.discord.util.EmbedBuilder;

public class ImageCmd extends AbstractCommand {

	private static final String API_URL = "https://www.deviantart.com/api/v1/oauth2/";

	private final RateLimiter rateLimiter;
	private String deviantArtToken;

	public ImageCmd() {
		super(Role.USER, "image");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}
		try {
			if(this.deviantArtToken == null) {
				this.generateAccessToken();
			}

			String encodedSearch = URLEncoder.encode(context.getArg(), "UTF-8");
			JSONObject resultObj = this.getRandomPopularResult(encodedSearch);

			if(resultObj == null) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No results for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			JSONObject authorObj = resultObj.getJSONObject("author");
			JSONObject contentObj = resultObj.getJSONObject("content");

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("DeviantArt Search (" + context.getArg() + ")")
					.withAuthorIcon(context.getGuild().getClient().getOurUser().getAvatarURL())
					.withUrl(resultObj.getString("url"))
					.withThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
					.withColor(Config.BOT_COLOR)
					.appendField("Title", resultObj.getString("title"), false)
					.appendField("Author", authorObj.getString("username"), false)
					.appendField("Category", resultObj.getString("category_path"), false)
					.withImage(contentObj.getString("src"));

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (SocketTimeoutException sterr) {
			BotUtils.sendMessage(Emoji.GEAR + " DeviantArt's servers are busy, please try again later.", context.getChannel());
			LogUtils.warn("SocketTimeoutException while getting an image from DeviantArt (" + sterr.getMessage() + ").");
		} catch (IOException e) {
			LogUtils.error("Something went wrong while getting an image... Please, try again later.", e, context.getChannel());
		}
	}

	private void generateAccessToken() throws JSONException, IOException {
		JSONObject oauthObj = new JSONObject(IOUtils.toString(new URL("https://www.deviantart.com/oauth2/token?"
				+ "client_id=" + Storage.getApiKey(ApiKeys.DEVIANTART_CLIENT_ID)
				+ "&client_secret=" + Storage.getApiKey(ApiKeys.DEVIANTART_API_SECRET)
				+ "&grant_type=client_credentials"), "UTF-8"));
		this.deviantArtToken = oauthObj.getString("access_token");
	}

	private JSONObject getRandomPopularResult(String encodedSearch) throws JSONException, IOException {
		JSONObject result = null;
		try {
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL(API_URL + "browse/popular?"
					+ "q=" + encodedSearch
					+ "&timerange=alltime"
					+ "&limit=1" // The pagination limit (min: 1 max: 50)
					+ "&offset=" + MathUtils.rand(150) // The pagination offset (min: 0 max: 50000)
					+ "&access_token=" + this.deviantArtToken), "UTF-8"));
			JSONArray resultsArray = mainObj.getJSONArray("results");

			if(resultsArray.length() != 0) {
				result = resultsArray.getJSONObject(MathUtils.rand(resultsArray.length()));
			}

		} catch (IOException e) {
			if(e.getMessage().contains("401")) {
				this.generateAccessToken();
				result = getRandomPopularResult(encodedSearch);
			}
		}

		return result;
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Search for a random image on DeviantArt.**")
				.appendField("Usage", context.getPrefix() + "image <search>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
