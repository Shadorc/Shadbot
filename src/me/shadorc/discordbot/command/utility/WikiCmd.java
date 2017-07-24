package me.shadorc.discordbot.command.utility;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;

public class WikiCmd extends Command {

	public WikiCmd() {
		super(false, "wiki", "wikipedia");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			BotUtils.sendMessage("Merci d'indiquer une recherche.", context.getChannel());
			return;
		}

		try {
			String searchEncoded = URLEncoder.encode(context.getArg(), "UTF-8");
			//Wiki api doc https://en.wikipedia.org/w/api.php?action=help&modules=query%2Bextracts
			String json = Infonet.getHTML(new URL("https://fr.wikipedia.org/w/api.php?"
					+ "action=query"
					+ "&titles=" + searchEncoded
					+ "&prop=extracts"
					+ "&format=json"
					+ "&explaintext=true"
					+ "&exintro=true"));

			JSONObject pagesObj = new JSONObject(json).getJSONObject("query").getJSONObject("pages");
			String pageId = pagesObj.names().getString(0);
			if(pageId.equals("-1")) {
				BotUtils.sendMessage("Aucun résultat pour : " + context.getArg(), context.getChannel());
				return;
			}
			String description = pagesObj.getJSONObject(pageId).getString("extract");
			BotUtils.sendMessage(description, context.getChannel());
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la récupération des informations sur Wikipédia.", e, context.getChannel());
		}
	}

}
