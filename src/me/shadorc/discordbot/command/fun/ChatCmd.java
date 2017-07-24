package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;

public class ChatCmd extends Command {

	private String aliceState = null;

	public ChatCmd() {
		super(false, "chat");
	}

	@Override
	public void execute(Context context) {
		this.answer(context.getArg(), context.getChannel());
	}

	private void answer(String arg, IChannel channel) {
		if(arg == null) {
			BotUtils.sendMessage(":grey_exclamation: Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
			return;
		}

		try {
			String xmlString = Infonet.getHTML(new URL("http://sheepridge.pandorabots.com/pandora/talk-xml?"
					+ "botid=b69b8d517e345aba"
					+ "&input=" + URLEncoder.encode(arg, "UTF-8")
					+ (aliceState != null ? "&custid=" + aliceState : "")));
			JSONObject result = XML.toJSONObject(xmlString).getJSONObject("result");
			String response = result.getString("that").replace("<br>", "\n").trim();
			aliceState = result.getString("custid");
			BotUtils.sendMessage(":speech_balloon: " + response, channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la discussion avec le bot.", e, channel);
		}
	}
}
