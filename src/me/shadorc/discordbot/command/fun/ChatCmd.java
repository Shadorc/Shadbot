package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;

import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.data.Config.APIKey;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import me.shadorc.discordbot.utils.NetUtils;
import me.shadorc.discordbot.utils.Utils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class ChatCmd extends AbstractCommand {

	private static final String API_KEY = Config.getAPIKey(APIKey.CLEVERBOT_API_KEY);
	private static final ConcurrentHashMap<IChannel, String> CHANNELS_CONV_ID = new ConcurrentHashMap<>();
	private static final ConcurrentHashMap<IChannel, String> CHANNELS_CUSTID = new ConcurrentHashMap<>();

	private final RateLimiter rateLimiter;

	public ChatCmd() {
		super(Role.USER, "chat");
		this.rateLimiter = new RateLimiter(2, ChronoUnit.SECONDS);
	}

	@Override
	public void execute(Context context) throws MissingArgumentException {
		if(!context.hasArg()) {
			throw new MissingArgumentException();
		}

		if(rateLimiter.isLimited(context.getGuild(), context.getAuthor())) {
			if(!rateLimiter.isWarned(context.getGuild(), context.getAuthor())) {
				rateLimiter.warn("Take it easy, don't spam :)", context);
			}
			return;
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
			LogUtils.warn("Something went wrong while discussing with A.L.I.C.E. Using Cleverbot instead."
					+ "\nError: " + err.getMessage()
					+ (mainObj == null ? "" : "\nJSON: " + mainObj));
		}

		try {
			CleverBotQuery bot = new CleverBotQuery(API_KEY, context.getArg());
			bot.setConversationID(CHANNELS_CONV_ID.get(context.getChannel()) == null ? "" : CHANNELS_CONV_ID.get(context.getChannel()));
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
				.appendField("Usage", context.getPrefix() + "chat <message>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
