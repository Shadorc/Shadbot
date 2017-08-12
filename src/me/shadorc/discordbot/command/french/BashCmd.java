package me.shadorc.discordbot.command.french;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.HtmlUtils;
import me.shadorc.discordbot.utils.Log;
import sx.blah.discord.util.EmbedBuilder;

public class BashCmd extends Command {

	public BashCmd() {
		super(false, "dtc", "bash");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		try {
			String json = HtmlUtils.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.getApiKey(ApiKeys.DTC_API_KEY)
					+ "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			BotUtils.sendMessage("```" + quote + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("An error occured while getting a quote from DansTonChat.com", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for /" + this.getNames()[0])
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Show a random quote from DansTonChat.com**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
