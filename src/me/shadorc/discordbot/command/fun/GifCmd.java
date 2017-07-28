package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;

public class GifCmd extends Command {

	public GifCmd() {
		super(false, "gif");
	}

	@Override
	public void execute(Context context) {
		if(context.getArg() == null) {
			try {
				String gifUrl = Infonet.parseHTML(new URL("http://gifland.us"), "<meta name=\"twitter:image:src", "content=\"", "\">");
				BotUtils.sendMessage(gifUrl, context.getChannel());
			} catch (IOException e) {
				Log.error("Une erreur est survenue lors de la récupération d'un gif sur Gifland.", e, context.getChannel());
			}
		}

		else {
			try {
				String json = Infonet.getHTML(new URL("https://api.giphy.com/v1/gifs/random?"
						+ "api_key=" + Storage.getApiKey(ApiKeys.GIPHY_API_KEY)
						+ "&tag=" + URLEncoder.encode(context.getArg(), "UTF-8")));
				JSONObject obj = new JSONObject(json);
				if(obj.get("data") instanceof JSONArray) {
					BotUtils.sendMessage(Emoji.WARNING + " Aucun résultat pour " + context.getArg(), context.getChannel());
					return;
				}
				String url = obj.getJSONObject("data").getString("url");
				BotUtils.sendMessage(url, context.getChannel());
			} catch (IOException e) {
				Log.error("Une erreur est survenue lors de la récupération d'un gif sur Giphy.", e, context.getChannel());
			}
		}
	}

}
