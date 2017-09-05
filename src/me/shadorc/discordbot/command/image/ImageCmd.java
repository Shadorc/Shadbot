package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class ImageCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;
	private String deviantArtToken;

	public ImageCmd() {
		super(Role.USER, "image");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
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

			EmbedBuilder builder = Utils.getDefaultEmbed()
					.withAuthorName("DeviantArt Search (" + context.getArg() + ")")
					.withUrl(resultObj.getString("url"))
					.withThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
					.appendField("Title", resultObj.getString("title"), false)
					.appendField("Author", authorObj.getString("username"), false)
					.appendField("Category", resultObj.getString("category_path"), false)
					.withImage(contentObj.getString("src"));

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting an image... Please, try again later.", err, context);
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
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
					+ "q=" + encodedSearch
					+ "&timerange=alltime"
					+ "&limit=1" // The pagination limit (min: 1 max: 50)
					+ "&offset=" + MathUtils.rand(150) // The pagination offset (min: 0 max: 50000)
					+ "&access_token=" + this.deviantArtToken), "UTF-8"));
			JSONArray resultsArray = mainObj.getJSONArray("results");

			if(resultsArray.length() != 0) {
				result = resultsArray.getJSONObject(MathUtils.rand(resultsArray.length()));
			}

		} catch (JSONException | IOException err) {
			if(err.getMessage().contains("401")) {
				this.generateAccessToken();
				result = getRandomPopularResult(encodedSearch);
			}
		}

		return result;
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Search for a random image on DeviantArt.**")
				.appendField("Usage", context.getPrefix() + "image <search>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
