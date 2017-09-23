package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
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
	private static final ConcurrentHashMap<IChannel, String> CHANNELS_CONV_ID = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<IChannel, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();

	private final RateLimiter rateLimiter;

	public ChatCmd() {
		super(CommandCategory.FUN, Role.USER, "chat");
		this.rateLimiter = new RateLimiter(RateLimiter.COMMON_COOLDOWN, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(rateLimiter.isSpamming(context)) {
			return;
		}

		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		JSONObject mainObj = null;
		try {
			String xmlString = NetUtils.getDoc("http://sheepridge.pandorabots.com/pandora/talk-xml?"
					+ "botid=b69b8d517e345aba"
					+ "&input=" + URLEncoder.encode(context.getArg(), "UTF-8")
					+ (CHANNELS_CUSTID.get(context.getChannel()) == null ? "" : "&custid=" + CHANNELS_CUSTID.get(context.getChannel())))
					.toString();
			mainObj = XML.toJSONObject(xmlString);
			JSONObject resultObj = mainObj.getJSONObject("result");
			String response = resultObj.getString("that").replace("<br>", "\n").replace("  ", " ").trim();
			CHANNELS_CUSTID.put(context.getChannel(), resultObj.getString("custid"));
			BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());
			return;
		} catch (JSONException | IOException err) {
			LogUtils.info("Something went wrong while discussing with A.L.I.C.E. (Error: " + err.getMessage() + "). Using Cleverbot instead."
					+ (mainObj == null ? "" : "\nJSON: " + mainObj));
		}

		try {
			CleverBotQuery bot = new CleverBotQuery(API_KEY, URLEncoder.encode(context.getArg(), "UTF-8"));
			bot.setConversationID(CHANNELS_CONV_ID.getOrDefault(context.getChannel(), ""));
			bot.sendRequest();
			CHANNELS_CONV_ID.put(context.getChannel(), bot.getConversationID());
			BotUtils.sendMessage(Emoji.SPEECH + " " + bot.getResponse(), context.getChannel());
		} catch (IOException err) {
			LogUtils.error("Something went wrong while discussing with Cleverbot... Please, try again later.", err, context);
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = Utils.getDefaultEmbed(this)
				.appendDescription("**Chat with the artificial intelligence A.L.I.C.E.**")
				.appendField("Usage", "`" + context.getPrefix() + "chat <message>`", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
