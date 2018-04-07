package me.shadorc.shadbot.command.image;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.LoadingMessage;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "gif" })
public class GifCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		LoadingMessage loadingMsg = new LoadingMessage("Loading gif...", context.getChannel());
		loadingMsg.send();

		try {
			String url = String.format("https://api.giphy.com/v1/gifs/random?api_key=%s&tag=%s",
					APIKeys.get(APIKey.GIPHY_API_KEY),
					NetUtils.encode(context.getArg()));

			String bodyText = NetUtils.getBody(url);

			// If the body is HTML, Giphy did not returned JSON
			if(bodyText.charAt(0) != '{') {
				throw new HttpStatusException("Giphy did not return valid JSON.", 503, url);
			}

			JSONObject mainObj = new JSONObject(bodyText);
			if(!mainObj.has("data")) {
				throw new HttpStatusException("Giphy did not return valid JSON.", 503, url);
			}

			if(mainObj.get("data") instanceof JSONArray) {
				loadingMsg.edit(TextUtils.noResult(context.getArg()));
				return;
			}

			EmbedBuilder embed = new EmbedBuilder()
					.withColor(Config.BOT_COLOR)
					.withImage(mainObj.getJSONObject("data").getString("image_url"));
			loadingMsg.edit(embed.build());

		} catch (JSONException | IOException err) {
			loadingMsg.delete();
			Utils.handle("getting a gif", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a random gif")
				.addArg("tag", "the tag to search", true)
				.setSource("https://giphy.com")
				.build();
	}

}
