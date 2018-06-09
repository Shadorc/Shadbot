package me.shadorc.shadbot.command.utils;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import discord4j.core.spec.EmbedCreateSpec;
import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "wiki", "wikipedia" })
public class WikiCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			// Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			String url = String.format("https://en.wikipedia.org/w/api.php?"
					+ "format=json"
					+ "&action=query"
					+ "&titles=%s"
					+ "&redirects=true"
					+ "&prop=extracts"
					+ "&explaintext=true"
					+ "&exintro=true"
					+ "&exsentences=5", NetUtils.encode(context.getArg().get()));
			JSONObject mainObj = new JSONObject(NetUtils.getJSON(url));

			JSONObject pagesObj = mainObj.getJSONObject("query").getJSONObject("pages");
			String pageId = pagesObj.names().getString(0);
			JSONObject resultObj = pagesObj.getJSONObject(pageId);

			if("-1".equals(pageId) || resultObj.getString("extract").isEmpty()) {
				loadingMsg.send(TextUtils.noResult(context.getArg().get()));
				return;
			}

			String extract = StringUtils.truncate(resultObj.getString("extract"), DiscordUtils.DESCRIPTION_CONTENT_LIMIT);

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed(String.format("Wikipedia: %s", resultObj.getString("title")),
					"https://upload.wikimedia.org/wikipedia/commons/thumb/4/4e/Mohapedia.png/842px-Mohapedia.png",
					String.format("https://en.wikipedia.org/wiki/%s", resultObj.getString("title").replace(" ", "_")))
					.setDescription(extract);
			loadingMsg.send(embed);

		} catch (JSONException | IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting Wikipedia information", context, err));
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Wikipedia description for a search.")
				.addArg("search", false)
				.build();
	}

}
