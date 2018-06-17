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
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.ExceptionUtils;
import me.shadorc.shadbot.utils.NetUtils;
import me.shadorc.shadbot.utils.StringUtils;
import me.shadorc.shadbot.utils.TextUtils;
import me.shadorc.shadbot.utils.embed.EmbedUtils;
import me.shadorc.shadbot.utils.embed.HelpBuilder;
import me.shadorc.shadbot.utils.object.message.LoadingMessage;
import reactor.core.publisher.Mono;

@RateLimited
@Command(category = CommandCategory.UTILS, names = { "urban" }, alias = "ud")
public class UrbanCmd extends AbstractCommand {

	@Override
	public void execute(Context context) {
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
			String definition = StringUtils.truncate(resultObj.getString("definition"), DiscordUtils.DESCRIPTION_CONTENT_LIMIT);
			String example = StringUtils.truncate(resultObj.getString("example"), DiscordUtils.FIELD_CONTENT_LIMIT);

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
	public Mono<EmbedCreateSpec> getHelp(Context context) {
		return new HelpBuilder(this, context)
				.setDescription("Show Urban Dictionary definition for a search.")
				.addArg("search", false)
				.build();
	}

}
