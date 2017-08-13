package me.shadorc.discordbot.command.fun;

import java.io.IOException;
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
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import sx.blah.discord.util.EmbedBuilder;

public class GifCmd extends Command {

	private final RateLimiter rateLimiter;

	public GifCmd() {
		super(false, "gif");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isLimitedAndNotWarned(context.getGuild(), context.getAuthor())) {
			rateLimiter.warn("Take it easy, don't spam :)", context);
			return;
		}

		try {
			String json = HtmlUtils.getHTML(new URL("https://api.giphy.com/v1/gifs/random?"
					+ "api_key=" + Storage.getApiKey(ApiKeys.GIPHY_API_KEY)
					+ ((context.getArg() == null) ? "" : "&tag=" + URLEncoder.encode(context.getArg(), "UTF-8"))));
			JSONObject obj = new JSONObject(json);
			if(obj.get("data") instanceof JSONArray) {
				BotUtils.sendMessage(Emoji.WARNING + " Aucun résultat pour " + context.getArg(), context.getChannel());
				return;
			}
			String url = obj.getJSONObject("data").getString("url");
			BotUtils.sendMessage(url, context.getChannel());
		} catch (IOException e) {
			Log.error("An arror occured while getting a gif from Giphy.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random gif or a gif corresponding to a tag.**")
				.appendField("Usage", context.getPrefix() + "gif or " + context.getPrefix() + "gif <tag>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
