package me.shadorc.discordbot;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;

public class CleverbotChat {

	private static String cleverbotState = null;
	private static boolean translationEnabled = false;

	public static void answer(String arg, IChannel channel) {
		if(arg == null) {
			Bot.sendMessage("Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
			return;
		}

		try {
			String input = arg;
			if(translationEnabled) input = Utils.translate("fr", "en", input);
			input = URLEncoder.encode(input, "UTF-8");
			String json = Infonet.getHTML(new URL("http://www.cleverbot.com/getreply?"
					+ "key=" + Storage.get(API_KEYS.CLEVERBOT_API_KEY) 
					+ "&input=" + input 
					+ (cleverbotState != null ? "&cs=" + cleverbotState : "")));
			JSONObject obj = new JSONObject(json);
			cleverbotState = obj.getString("cs");
			String response = obj.getString("output");
			if(translationEnabled) response = Utils.translate("en", "fr", response);
			Bot.sendMessage(response, channel);
		} catch (IOException e) {
			Utils.error(e, "Une erreur est survenue lors de la discussion avec le bot.", channel);
		}
	}

	public static void setTranslationEnabled(boolean _translationEnabled) {
		translationEnabled = _translationEnabled;
	}
}
