package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.util.EmbedBuilder;

public class WikiCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public WikiCmd() {
		super(CommandCategory.UTILS, Role.USER, "wiki", "wikipedia");
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
			// Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			JSONObject mainObj = new JSONObject(NetUtils.getBody("https://en.wikipedia.org/w/api.php?"
					+ "format=json"
					+ "&action=query"
					+ "&titles=" + URLEncoder.encode(context.getArg(), "UTF-8")
					+ "&redirects=true"
					+ "&prop=extracts"
					+ "&explaintext=true"
					+ "&exintro=true"
					+ "&exsentences=5"));

			JSONObject pagesObj = mainObj.getJSONObject("query").getJSONObject("pages");
			String pageId = pagesObj.names().getString(0);
			JSONObject resultObj = pagesObj.getJSONObject(pageId);

			if("-1".equals(pageId) || resultObj.getString("extract").isEmpty()) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			String extract = resultObj.getString("extract");
			if(extract.length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				extract = extract.substring(0, EmbedBuilder.DESCRIPTION_CONTENT_LIMIT - 3) + "...";
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Wikipedia")
					.withAuthorIcon("https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Mohapedia.png/842px-Mohapedia.png")
					.withUrl("https://fr.wikipedia.org/wiki/" + resultObj.getString("title").replace(" ", "_"))
					.withColor(Config.BOT_COLOR)
					.withTitle(resultObj.getString("title"))
					.appendDescription(extract);
			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.manageException("getting Wikipedia information", context, err);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Show Wikipedia description for a search.**")
				.appendField("Usage", "`" + context.getPrefix() + "wiki <search>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}

}
