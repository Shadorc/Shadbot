package me.shadorc.discordbot.command;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.Bot;
import me.shadorc.discordbot.storage.Storage;
import me.shadorc.discordbot.storage.Storage.API_KEYS;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;

public class Chat {

	public enum ChatBot {
		CLEVERBOT, ALICE;
	}

	private static ChatBot chatBot = ChatBot.ALICE;
	private static String cleverbotState = null;
	private static String aliceState = null;

	public static void answer(String arg, IChannel channel) {
		if(arg == null) {
			Bot.sendMessage("Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
			return;
		}

		try {
			if(chatBot.equals(ChatBot.ALICE)) {
				String xmlString = Infonet.getHTML(new URL("http://sheepridge.pandorabots.com/pandora/talk-xml?"
						+ "botid=b69b8d517e345aba"
						+ "&input=" + URLEncoder.encode(arg, "UTF-8")
						+ (aliceState != null ? "&custid=" + aliceState : "")));
				JSONObject result = XML.toJSONObject(xmlString).getJSONObject("result");
				String response = result.getString("that").replace("<br>", "\n").trim();
				aliceState = result.getString("custid");
				Bot.sendMessage(response, channel);
			}

			else if(chatBot.equals(ChatBot.CLEVERBOT)) {
				String jsonString = Infonet.getHTML(new URL("http://www.cleverbot.com/getreply?"
						+ "key=" + Storage.get(API_KEYS.CLEVERBOT_API_KEY)
						+ "&input=" + URLEncoder.encode(arg, "UTF-8")
						+ (cleverbotState != null ? "&cs=" + cleverbotState : "")));
				JSONObject obj = new JSONObject(jsonString);
				String response = obj.getString("output");
				cleverbotState = obj.getString("cs");
				Bot.sendMessage(response, channel);
			}
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la discussion avec le bot.", e, channel);
		}
	}

	public static void setChatbot(ChatBot _chatBot) {
		chatBot = _chatBot;
	}
}
