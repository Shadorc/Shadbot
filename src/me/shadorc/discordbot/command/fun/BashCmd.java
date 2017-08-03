package me.shadorc.discordbot.command.fun;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;

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
	public void execute(Context context) {
		try {
			String json = HtmlUtils.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.getApiKey(ApiKeys.DTC_API_KEY)
					+ "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			BotUtils.sendMessage("```" + quote + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération d'une quote sur danstonchat.com", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Aide pour la commande /" + context.getArg())
				.withAuthorIcon(context.getClient().getOurUser().getAvatarURL())
				.withColor(new Color(170, 196, 222))
				.appendDescription("**Affiche une quote aléatoire issue du site danstonchat.com.**");
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
