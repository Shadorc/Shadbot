package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;

import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.CommandCategory;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.command.Role;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.ExceptionUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import me.shadorc.discordbot.utils.command.Emoji;
import me.shadorc.discordbot.utils.command.MissingArgumentException;
import me.shadorc.discordbot.utils.command.RateLimiter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class ChatCmd extends AbstractCommand {

	private static final String API_KEY = Config.get(APIKey.CLEVERBOT_API_KEY);
	private static final ConcurrentHashMap<Long, String> CHANNELS_CONV_ID = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<Long, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();

	public ChatCmd() {
		super(CommandCategory.FUN, Role.USER, RateLimiter.DEFAULT_COOLDOWN, "chat");
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		String response;

		try {
			response = this.talk(context.getChannel(), "b0dafd24ee35a477", context.getArg());
			BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());
		} catch (JSONException | IOException err) {
			LogUtils.info("Chomsky is not reachable, using Marvin instead.");
		}

		try {
			response = this.talk(context.getChannel(), "efc39100ce34d038", context.getArg());
			BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());
		} catch (JSONException | IOException err) {
			LogUtils.info("Marvin is not reachable, using Cleverbot instead.");
		}

		try {
			CleverBotQuery bot = new CleverBotQuery(API_KEY, URLEncoder.encode(context.getArg(), "UTF-8"));
			bot.setConversationID(CHANNELS_CONV_ID.getOrDefault(context.getChannel().getLongID(), ""));
			bot.sendRequest();
			CHANNELS_CONV_ID.put(context.getChannel().getLongID(), bot.getConversationID());
			BotUtils.sendMessage(Emoji.SPEECH + " " + bot.getResponse(), context.getChannel());
		} catch (IOException err) {
			ExceptionUtils.manageException("discussing with Cleverbot", context, err);
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
