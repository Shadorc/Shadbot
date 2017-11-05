package me.shadorc.discordbot.command.image;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

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
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.TextUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class GifCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public GifCmd() {
		super(CommandCategory.IMAGE, Role.USER, "gif");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		try {
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://api.giphy.com/v1/gifs/random?"
					+ "api_key=" + Config.get(APIKey.GIPHY_API_KEY)
					+ (context.hasArg() ? "&tag=" + URLEncoder.encode(context.getArg(), "UTF-8") : "")));

			if(mainObj.get("data") instanceof JSONArray) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			EmbedBuilder embed = new EmbedBuilder()
					.withColor(Config.BOT_COLOR)
					.withImage(mainObj.getJSONObject("data").getString("image_url"));
			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting a gif", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show a random gif.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " [<tag>]`", false)
				.appendField("Argument", "**tag** - [OPTIONAL] the tag to search", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
