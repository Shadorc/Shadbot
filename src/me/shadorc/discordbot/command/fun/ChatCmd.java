package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.command.Command;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utility.BotUtils;
import me.shadorc.discordbot.utility.Log;
import me.shadorc.discordbot.utility.NetUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;

public class ChatCmd extends Command {

	private static final Map<IGuild, String> GUILDS_CUSTID = new HashMap<>();

	public ChatCmd() {
		super(false, "chat");
	}

	@Override
	public void execute(Context context) {
		if(!GUILDS_CUSTID.containsKey(context.getGuild())) {
			GUILDS_CUSTID.put(context.getGuild(), null);
		}
		this.answer(context.getArg(), context.getChannel());
	}

	private void answer(String arg, IChannel channel) {
		if(arg == null) {
			BotUtils.sendMessage(Emoji.WARNING + " Une conversation fonctionne mieux quand on dit quelque chose :)", channel);
			return;
		}

		try {
			String aliceState = GUILDS_CUSTID.get(channel.getGuild());
			String xmlString = NetUtils.getHTML(new URL("http://sheepridge.pandorabots.com/pandora/talk-xml?"
					+ "botid=b69b8d517e345aba"
					+ "&input=" + URLEncoder.encode(arg, "UTF-8")
					+ (aliceState != null ? "&custid=" + aliceState : "")));
			JSONObject result = XML.toJSONObject(xmlString).getJSONObject("result");
			String response = result.getString("that").replace("<br>", "\n").trim();
			GUILDS_CUSTID.put(channel.getGuild(), result.getString("custid"));
			BotUtils.sendMessage(Emoji.SPEECH + " " + response, channel);
		} catch (SocketTimeoutException e) {
			BotUtils.sendMessage(Emoji.WARNING + " La discussion n'est pas disponible actuellement.", channel);
			Log.warn("A.L.I.C.E. ne répond pas.");
		} catch (IOException e) {
			Log.error("Une erreur est survenue lors de la discussion avec le bot.", e, channel);
		}
	}
}
