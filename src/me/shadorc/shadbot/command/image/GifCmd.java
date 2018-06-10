package me.shadorc.shadbot.command.image;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.data.APIKeys;
import me.shadorc.shadbot.data.APIKeys.APIKey;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "gif" })
public class GifCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String url = String.format("https://api.giphy.com/v1/gifs/random?api_key=%s&tag=%s",
					APIKeys.get(APIKey.GIPHY_API_KEY),
					NetUtils.encode(context.getArg().orElse("")));

			JSONObject mainObj = new JSONObject(NetUtils.getJSON(url));
			if(!mainObj.has("data")) {
				throw new HttpStatusException("Giphy did not return valid JSON.", NetUtils.JSON_ERROR_CODE, url);
			}

			if(mainObj.get("data") instanceof JSONArray) {
				loadingMsg.send(TextUtils.noResult(context.getArg().orElse("random search")));
				return;
			}

			EmbedCreateSpec embed = new EmbedCreateSpec()
					.setColor(Config.BOT_COLOR.getRGB())
					.setImage(mainObj.getJSONObject("data").getString("image_url"));
			loadingMsg.send(embed);

		} catch (JSONException | IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting a gif", context, err));
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show a random gif")
				.addArg("tag", "the tag to search", true)
				.setSource("https://giphy.com")
				.build();
	}

}
