package me.shadorc.discordbot.command;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.Command;
import me.shadorc.discordbot.Context;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.API_KEYS;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;

public class ChatCommand extends Command {

	public enum ChatBot {
		CLEVERBOT, ALICE;
	}

	private ChatBot chatBot = ChatBot.ALICE;
	private String cleverbotState = null;
	private String aliceState = null;

	public ChatCommand() {
		super("chat");
	}

	@Override
	public void execute(Context context) {
		this.answer(context.getArg(), context.getChannel());
	}

	private void answer(String arg, IChannel channel) {
		if(arg == null) {
			BotUtils.sendMessage("Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
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
				BotUtils.sendMessage(response, channel);
			}

			else if(chatBot.equals(ChatBot.CLEVERBOT)) {
				String jsonString = Infonet.getHTML(new URL("http://www.cleverbot.com/getreply?"
						+ "key=" + Storage.get(API_KEYS.CLEVERBOT_API_KEY)
						+ "&input=" + URLEncoder.encode(arg, "UTF-8")
						+ (cleverbotState != null ? "&cs=" + cleverbotState : "")));
				JSONObject obj = new JSONObject(jsonString);
				String response = obj.getString("output");
				cleverbotState = obj.getString("cs");
				BotUtils.sendMessage(response, channel);
			}
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la discussion avec le bot.", e, channel);
		}
	}
	
	public void setChatbot(ChatBot chatBot) {
		this.chatBot = chatBot;
	}
}
