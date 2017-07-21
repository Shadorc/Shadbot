package me.shadorc.discordbot.command;

import java.io.IOException;
import java.net.URL;

import org.json.JSONArray;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;

public class BashCommand extends Command {

	public BashCommand() {
		super("dtc", "bash");
	}

	@Override
	public void execute(Context context) {
		try {
			String json = Infonet.getHTML(new URL("http://api.danstonchat.com/0.3/view/random?"
					+ "key=" + Storage.get(API_KEYS.DTC_API_KEY)
					+ "&format=json"));
			String quote = new JSONArray(json).getJSONObject(0).getString("content");
			BotUtils.sendMessage("```" + quote + "```", context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération d'une quote sur danstonchat.com", e, context.getChannel());
		}		
	}

}
