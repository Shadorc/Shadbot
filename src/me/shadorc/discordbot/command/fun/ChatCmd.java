package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.infonet.Infonet;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class ChatCmd extends Command {

	private static Map<IGuild, String> GUILDS = new HashMap<>();

	public ChatCmd() {
		super(false, "chat");
	}

	@Override
	public void execute(Context context) {
		if(!GUILDS.containsKey(context.getGuild())) {
			GUILDS.put(context.getGuild(), null);
		}
		this.answer(context.getArg(), context.getChannel());
	}

	private void answer(String arg, IChannel channel) {
		if(arg == null) {
			BotUtils.sendMessage(":grey_exclamation: Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
			return;
		}

		try {
			String aliceState = GUILDS.get(channel.getGuild());
			String xmlString = Infonet.getHTML(new URL("http://sheepridge.pandorabots.com/pandora/talk-xml?"
					+ "botid=b69b8d517e345aba"
					+ "&input=" + URLEncoder.encode(arg, "UTF-8")
					+ (aliceState != null ? "&custid=" + aliceState : "")));
			JSONObject result = XML.toJSONObject(xmlString).getJSONObject("result");
			String response = result.getString("that").replace("<br>", "\n").trim();
			GUILDS.put(channel.getGuild(), result.getString("custid"));
			BotUtils.sendMessage(":speech_balloon: " + response, channel);
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la discussion avec le bot.", e, channel);
		}
	}
}
