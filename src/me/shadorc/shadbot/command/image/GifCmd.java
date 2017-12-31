package me.shadorc.shadbot.command.image;

import java.io.IOException;

import org.json.JSONArray;
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
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.IMAGE, names = { "gif" })
public class GifCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			String url = String.format("https://api.giphy.com/v1/gifs/random?api_key=%s&tag=%s",
					APIKeys.get(APIKey.GIPHY_API_KEY),
					context.hasArg() ? NetUtils.encode(context.getArg()) : "");

			JSONObject mainObj = new JSONObject(NetUtils.getBody(url));
			if(mainObj.get("data") instanceof JSONArray) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
					.withImage(mainObj.getJSONObject("data").getString("image_url"));
			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.handle("getting a gif", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show a random gif")
				.addArg("tag", "the tag to search", true)
				.build();
	}

}
