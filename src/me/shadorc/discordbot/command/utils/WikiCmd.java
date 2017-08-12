package me.shadorc.discordbot.command.utils;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Log;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import sx.blah.discord.util.EmbedBuilder;

public class WikiCmd extends Command {

	public WikiCmd() {
		super(false, "wiki", "wikipedia");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		try {
			String searchEncoded = URLEncoder.encode(context.getArg(), "UTF-8");
			// Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			String jsonStr = HtmlUtils.getHTML(new URL("https://en.wikipedia.org/w/api.php?"
					+ "action=query"
					+ "&titles=" + searchEncoded
					+ "&prop=extracts"
					+ "&format=json"
					+ "&explaintext=true"
					+ "&exintro=true"
					+ "&exsentences=5"));

			JSONObject mainObj = new JSONObject(jsonStr).getJSONObject("query").getJSONObject("pages");
			String pageId = mainObj.names().getString(0);
			JSONObject resultObj = mainObj.getJSONObject(pageId);

			if(pageId.equals("-1") || resultObj.getString("extract").isEmpty()) {
				BotUtils.sendMessage(Emoji.WARNING + " No result for : " + context.getArg(), context.getChannel());
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

		} catch (IOException e) {
			Log.error("An error occured while getting Wikipedia information.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.withDescription("**Show Wikipedia description for a search.**")
				.appendField("Usage", "/wiki <search>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
