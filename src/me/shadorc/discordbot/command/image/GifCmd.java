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
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.util.EmbedBuilder;

public class GifCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public GifCmd() {
		super(Role.USER, "gif");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
		}

		try {
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://api.giphy.com/v1/gifs/random?"
					+ "api_key=" + Config.getAPIKey(APIKey.GIPHY_API_KEY)
					+ (context.hasArg() ? "&tag=" + URLEncoder.encode(context.getArg(), "UTF-8") : "")), "UTF-8"));

			if(mainObj.get("data") instanceof JSONArray) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			EmbedBuilder embed = new EmbedBuilder()
					.withColor(Config.BOT_COLOR)
					.withImage(mainObj.getJSONObject("data").getString("image_url"));
			BotUtils.sendEmbed(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting a gif from Giphy.... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random gif or a gif corresponding to a tag.**")
				.appendField("Usage", context.getPrefix() + "gif"
						+ "\n" + context.getPrefix() + "gif <tag>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
