package me.shadorc.shadbot.command.image;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.LogUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.TimeUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "image" })
public class ImageCmd extends AbstractCommand {

	private String accessToken;
	private int expiresIn;
	private long lastTokenGeneration;

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		LoadingMessage loadingMsg = new LoadingMessage("Loading image...", context.getChannel());
		loadingMsg.send();

		try {
			JSONObject resultObj = this.getRandomPopularResult(NetUtils.encode(context.getArg()));
			if(resultObj == null) {
				loadingMsg.edit(TextUtils.noResult(context.getArg()));
				return;
			}

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.withAuthorName("DeviantArt (Search: " + context.getArg() + ")")
					.withAuthorUrl(resultObj.getString("url"))
					.withThumbnail("http://www.pngall.com/wp-content/uploads/2016/04/Deviantart-Logo-Transparent.png")
					.appendField("Title", resultObj.getString("title"), false)
					.appendField("Author", resultObj.getJSONObject("author").getString("username"), false)
					.appendField("Category", resultObj.getString("category_path"), false)
					.withImage(resultObj.getJSONObject("content").getString("src"));

			loadingMsg.edit(embed.build());

		} catch (JSONException | IOException err) {
			loadingMsg.delete();
			Utils.handle("getting an image", context, err);
		}
	}

	private JSONObject getRandomPopularResult(String encodedSearch) throws JSONException, IOException {
		try {
			if(TimeUtils.getMillisUntil(lastTokenGeneration) >= TimeUnit.SECONDS.toMillis(expiresIn)) {
				this.generateAccessToken();
			}

			String url = String.format("https://www.deviantart.com/api/v1/oauth2/browse/popular?"
					+ "q=%s"
					+ "&timerange=alltime"
					+ "&limit=25" // The pagination limit (min: 1 max: 50)
					+ "&offset=%d" // The pagination offset (min: 0 max: 50000)
					+ "&access_token=%s",
					encodedSearch, ThreadLocalRandom.current().nextInt(150), this.accessToken);

			JSONObject mainObj = new JSONObject(NetUtils.getBody(url));

			List<JSONObject> results = Utils.toList(mainObj.getJSONArray("results"), JSONObject.class).stream()
					.filter(obj -> obj.has("content")).collect(Collectors.toList());

			if(results.isEmpty()) {
				return null;
			}

			return results.get(ThreadLocalRandom.current().nextInt(results.size()));

		} catch (JSONException | IOException err) {
			return null;
		}
	}

	private void generateAccessToken() throws JSONException, IOException {
		String url = String.format("https://www.deviantart.com/oauth2/token?client_id=%s&client_secret=%s&grant_type=client_credentials",
				APIKeys.get(APIKey.DEVIANTART_CLIENT_ID),
				APIKeys.get(APIKey.DEVIANTART_API_SECRET));
		JSONObject oauthObj = new JSONObject(NetUtils.getBody(url));
		this.accessToken = oauthObj.getString("access_token");
		this.expiresIn = oauthObj.getInt("expires_in");
		this.lastTokenGeneration = System.currentTimeMillis();
		LogUtils.infof("DeviantArt token generated: %s", this.accessToken);
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Search for a random image on DeviantArt.")
				.addArg("search", false)
				.setSource("https://www.deviantart.com")
				.build();
	}
}
