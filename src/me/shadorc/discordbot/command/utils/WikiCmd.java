package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.util.EmbedBuilder;

public class WikiCmd extends AbstractCommand {

	private final RateLimiter rateLimiter;

	public WikiCmd() {
		super(Role.USER, "wiki", "wikipedia");
		this.rateLimiter = new RateLimiter(5, ChronoUnit.SECONDS);
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
			// Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			JSONObject mainObj = new JSONObject(IOUtils.toString(new URL("https://en.wikipedia.org/w/api.php?"
					+ "format=json"
					+ "&action=query"
					+ "&titles=" + URLEncoder.encode(context.getArg(), "UTF-8")
					+ "&redirects=true"
					+ "&prop=extracts"
					+ "&explaintext=true"
					+ "&exintro=true"
					+ "&exsentences=5"), "UTF-8"));

			JSONObject pagesObj = mainObj.getJSONObject("query").getJSONObject("pages");
			String pageId = pagesObj.names().getString(0);
			JSONObject resultObj = pagesObj.getJSONObject(pageId);

			if("-1".equals(pageId) || resultObj.getString("extract").isEmpty()) {
				BotUtils.sendMessage(Emoji.MAGNIFYING_GLASS + " No result for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName(resultObj.getString("title"))
					.withUrl("https://fr.wikipedia.org/wiki/" + resultObj.getString("title").replace(" ", "_"))
					.withThumbnail("https://s1.qwant.com/thumbr/300x0/2/8/50c4ce83955fe31f8f070e40c10926/b_0_q_0_p_0.jpg?u=https%3A%2F%2Fupload.wikimedia.org%2Fwikipedia%2Fcommons%2Fthumb%2Fd%2Fd1%2FWikipedia-logo-v2-fr.svg%2F892px-Wikipedia-logo-v2-fr.svg.png&q=0&b=0&p=0&a=0")
					.withAuthorIcon(context.getAuthor().getAvatarURL())
					.withColor(Config.BOT_COLOR)
					.appendDesc(resultObj.getString("extract"));
			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			LogUtils.error("Something went wrong while getting Wikipedia information... Please, try again later.", err, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDescription("**Show Wikipedia description for a search.**")
				.appendField("Usage", context.getPrefix() + "wiki <search>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
