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
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "urban" }, alias = "ud")
public class UrbanCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		context.requireArg();

		LoadingMessage loadingMsg = new LoadingMessage(context.getClient(), context.getChannelId());

		try {
			String url = String.format("https://api.urbandictionary.com/v0/define?term=%s", NetUtils.encode(context.getArg().get()));
			JSONObject mainObj = new JSONObject(NetUtils.getJSON(url));

			if(mainObj.getString("result_type").equals("no_results")) {
				loadingMsg.send(TextUtils.noResult(context.getArg().get()));
				return;
			}

			JSONObject resultObj = mainObj.getJSONArray("list").getJSONObject(0);
			String definition = StringUtils.truncate(resultObj.getString("definition"), Utils.DESCRIPTION_CONTENT_LIMIT);
			String example = StringUtils.truncate(resultObj.getString("example"), Utils.FIELD_CONTENT_LIMIT);

			EmbedCreateSpec embed = EmbedUtils.getDefaultEmbed("Urban Dictionary: " + resultObj.getString("word"),
					resultObj.getString("permalink"))
					.setThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.setDescription(definition)
					.addField("Example", example, false);

			loadingMsg.send(embed);

		} catch (JSONException | IOException err) {
			loadingMsg.send(ExceptionUtils.handleAndGet("getting Urban Dictionary definition", context, err));
		}
	}

	@Override
	public EmbedCreateSpec getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Urban Dictionary definition for a search.")
				.addArg("search", false)
				.build();
	}

}
