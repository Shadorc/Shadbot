package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.MathUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class ImageCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;
	private String deviantArtToken;

	private int expiresIn;
	private long tokenRequestTime;

	public ImageCmd() {
		super(CommandCategory.IMAGE, Role.USER, "image");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			if(this.deviantArtToken == null) {
				this.generateAccessToken();
			}

			String encodedSearch = URLEncoder.encode(context.getArg(), "UTF-8");
			JSONObject resultObj = this.getRandomPopularResult(encodedSearch);

			if(resultObj == null) {
				BotUtils.send(Emoji.MAGNIFYING_GLASS + " No results for \"" + context.getArg() + "\"", context.getChannel());
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

			BotUtils.send(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting an image... Please, try again later.", err, context);
		}
	}

	private JSONObject getRandomPopularResult(String encodedSearch) throws JSONException, IOException {
		try {
			if(this.isTokenExpired()) {
				this.generateAccessToken();
			}

			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
					+ "q=" + encodedSearch
					+ "&timerange=alltime"
					+ "&limit=25" // The pagination limit (min: 1 max: 50)
					+ "&offset=" + MathUtils.rand(150) // The pagination offset (min: 0 max: 50000)
					+ "&access_token=" + this.deviantArtToken));
			JSONArray resultsArray = mainObj.getJSONArray("results");

			JSONObject resultObj;
			do {
				if(resultsArray.length() == 0) {
					return null;
				}

				int index = MathUtils.rand(resultsArray.length());
				resultObj = resultsArray.getJSONObject(index);
				resultsArray.remove(index);
			} while(!resultObj.has("content"));

			return resultObj;

		} catch (JSONException | IOException err) {
			return null;
		}
	}

	private void generateAccessToken() throws JSONException, IOException {
		JSONObject oauthObj = new JSONObject(NetUtils.getBody("https://www.deviantart.com/oauth2/token?"
				+ "client_id=" + Config.get(APIKey.DEVIANTART_CLIENT_ID)
				+ "&client_secret=" + Config.get(APIKey.DEVIANTART_API_SECRET)
				+ "&grant_type=client_credentials"));
		this.deviantArtToken = oauthObj.getString("access_token");
		this.expiresIn = oauthObj.getInt("expires_in");
		this.tokenRequestTime = System.currentTimeMillis();
	}

	private boolean isTokenExpired() {
		return (System.currentTimeMillis() - tokenRequestTime) < TimeUnit.SECONDS.toMillis(expiresIn);
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Search for a random image on DeviantArt.**")
				.appendField("Usage", "`" + context.getPrefix() + "image <search>`", false);
		BotUtils.send(builder.build(), context.getChannel());
	}
}
