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

public class UrbanCmd extends Command {

	public UrbanCmd() {
		super(false, "urban", "ud");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(context.getArg() == null) {
			throw new MissingArgumentException();
		}

		try {
			String jsonStr = HtmlUtils.getHTML(new URL("https://api.urbandictionary.com/v0/define?"
					+ "term=" + URLEncoder.encode(context.getArg(), "UTF-8")));
			JSONObject mainObj = new JSONObject(jsonStr);

			if(mainObj.getString("result_type").equals("no_results")) {
				BotUtils.sendMessage(Emoji.WARNING + " No results for \"" + context.getArg() + "\"", context.getChannel());
				return;
			}

			JSONObject resultObj = mainObj.getJSONArray("list").getJSONObject(0);

			String definition = resultObj.getString("definition");
			if(definition.length() > EmbedBuilder.DESCRIPTION_CONTENT_LIMIT) {
				definition = definition.substring(0, EmbedBuilder.DESCRIPTION_CONTENT_LIMIT - 3) + "...";
			}

			EmbedBuilder builder = new EmbedBuilder()
					.withAuthorName("Urban Dictionary: " + resultObj.getString("word"))
					.withAuthorIcon(context.getGuild().getClient().getOurUser().getAvatarURL())
					.withThumbnail("http://www.packal.org/sites/default/files/public/styles/icon_large/public/workflow-files/florianurban/icon/icon.png")
					.withColor(Config.BOT_COLOR)
					.withDescription(definition);

			String example = resultObj.getString("example");
			if(!example.isEmpty()) {
				builder.appendField("Example", resultObj.getString("example"), false);
			}

			BotUtils.sendEmbed(builder.build(), context.getChannel());

		} catch (IOException e) {
			Log.error("An error occured while getting Urban Dictionary definition.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Search a definition of a word with Urban Dictionary.**")
				.appendField("Usage", context.getPrefix() + "urban <word>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}

}
