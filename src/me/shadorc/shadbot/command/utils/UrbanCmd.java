package me.shadorc.shadbot.command.utils;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import me.shadorc.shadbot.core.command.AbstractCommand;
import me.shadorc.shadbot.core.command.CommandCategory;
import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.core.command.annotation.Command;
import me.shadorc.shadbot.core.command.annotation.RateLimited;
import me.shadorc.shadbot.exception.MissingArgumentException;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.Utils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.util.EmbedBuilder;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "urban" }, alias = "ud")
public class UrbanCmd extends AbstractCommand {

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		try {
			String url = String.format("https://api.urbandictionary.com/v0/define?term=%s", NetUtils.encode(context.getArg()));
			JSONObject mainObj = new JSONObject(NetUtils.getJSON(url));
			
			JSONArray list = mainObj.getJSONArray("list");

			if(list.toList().isEmpty()) {
				BotUtils.sendMessage(TextUtils.noResult(context.getArg()), context.getChannel());
				return;
			}

			JSONObject resultObj = list.getJSONObject(0);
			String definition = StringUtils.truncate(resultObj.getString("definition"), EmbedBuilder.DESCRIPTION_CONTENT_LIMIT);
			String example = StringUtils.truncate(resultObj.getString("example"), EmbedBuilder.FIELD_CONTENT_LIMIT);

			EmbedBuilder embed = EmbedUtils.getDefaultEmbed()
					.setLenient(true)
					.withAuthorName("Urban Dictionary: " + resultObj.getString("word"))
					.withAuthorUrl(resultObj.getString("permalink"))
					.withThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.appendDescription(definition)
					.appendField("Example", example, false);

			BotUtils.sendMessage(embed.build(), context.getChannel());

		} catch (JSONException | IOException err) {
			Utils.handle("getting Urban Dictionary definition", context, err);
		}
	}

	@Override
	public EmbedObject getHelp(String prefix) {
		return new HelpBuilder(this, prefix)
				.setDescription("Show Urban Dictionary definition for a search.")
				.addArg("search", false)
				.build();
	}

}
