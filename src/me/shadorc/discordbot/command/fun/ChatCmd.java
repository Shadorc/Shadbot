package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class ChatCmd extends AbstractCommand {

	private static final ConcurrentHashMap<Long, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();
	private static final List<String> BOTS_ID = Arrays.asList("efc39100ce34d038", "b0dafd24ee35a477", "ea373c261e3458c6", "b0a6a41a5e345c23");

	private static int errorCount;

	public ChatCmd() {
		super(CommandCategory.FUN, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "chat");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String response = null;
		for(String botID : BOTS_ID) {
			try {
				response = this.talk(context.getChannel(), botID, context.getArg());
				BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());
				errorCount = 0;
				break;
			} catch (JSONException | IOException err) {
				LogUtils.info("{" + this.getClass().getSimpleName() + "} " + botID + " is not reachable, trying another one.");
			}
		}

		if(response == null) {
			BotUtils.sendMessage(Emoji.SLEEPING + " Sorry, A.L.I.C.E. seems to be AFK, she'll probably come back later.", context.getChannel());
			errorCount++;
			if(errorCount >= 5) {
				LogUtils.error("No artificial intelligence is responding (Error count: " + errorCount + ").");
			}
		}
	}

	private String talk(IChannel channel, String botID, String input) throws UnsupportedEncodingException, IOException {
		String custid = CHANNELS_CUSTID.get(channel.getLongID());
		String xmlString = NetUtils.getDoc("https://www.pandorabots.com/pandora/talk-xml?"
				+ "botid=" + botID
				+ "&input=" + URLEncoder.encode(input, "UTF-8")
				+ (custid == null ? "" : "&custid=" + custid))
				.toString();
		JSONObject mainObj = XML.toJSONObject(xmlString);
		JSONObject resultObj = mainObj.getJSONObject("result");
		CHANNELS_CUSTID.put(channel.getLongID(), resultObj.getString("custid"));
		return resultObj.getString("that").replace("<br>", "\n").replace("  ", " ").trim();
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Chat with an artificial intelligence.**")
				.appendField("Usage", "`" + context.getPrefix() + this.getFirstName() + " <message>`", false);
		BotUtils.sendMessage(builder.build(), context.getChannel());
	}
}
