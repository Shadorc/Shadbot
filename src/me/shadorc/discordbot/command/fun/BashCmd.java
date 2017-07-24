package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;

import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;

public class BashCmd extends Command {

	public BashCmd() {
		super(false, "dtc", "bash");
	}

	@Override
	public void execute(Context context) {
		try {
			String json = Infonet.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.get(ApiKeys.DTC_API_KEY)
					+ "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			BotUtils.sendMessage("```" + quote + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération d'une quote sur danstonchat.com", e, context.getChannel());
		}
	}

}
