package me.shadorc.shadbot.command.utils;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@Command(category = CommandCategory.UTILS, names = { "urban" }, alias = "ud")
public class UrbanCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			String url = String.format("https://api.urbandictionary.com/v0/define?term=%s", NetUtils.encode(context.getArg()));
			JSONObject mainObj = new JSONObject(NetUtils.getBody(url));

			if(mainObj.getString("result_type").equals("no_results")) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			JSONObject resultObj = mainObj.getJSONArray("list").getJSONObject(0);
			String definition = StringUtils.truncate(resultObj.getString("definition"), EmbedBuilder.DESCRIPTION_CONTENT_LIMIT);
			String example = StringUtils.truncate(resultObj.getString("example"), EmbedBuilder.FIELD_CONTENT_LIMIT);

			EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Urban Dictionary: " + resultObj.getString("word"))
					.withUrl(resultObj.getString("permalink"))
					.withThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.appendDescription(definition)
					.appendField("Example", example, false);

			BotUtils.sendMessage(builder.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			ExceptionUtils.handle("getting Urban Dictionary definition", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(Context context) {
		return new HelpBuilder(this, context.getPrefix())
				.setDescription("Show Urban Dictionary definition for a search.")
				.appendArg("search", false)
				.build();
	}

}
