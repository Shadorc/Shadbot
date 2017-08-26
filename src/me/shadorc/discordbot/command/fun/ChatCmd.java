package me.shadorc.discordbot.command.fun;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import com.michaelwflaherty.cleverbotapi.CleverBotQuery;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.MissingArgumentException;
import me.shadorc.discordbot.RateLimiter;
import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.Storage;
import me.shadorc.discordbot.Storage.ApiKeys;
import me.shadorc.discordbot.command.AbstractCommand;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.utils.BotUtils;
import me.shadorc.discordbot.utils.LogUtils;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class ChatCmd extends AbstractCommand {

	private static final String API_KEY = Storage.getApiKey(ApiKeys.CLEVERBOT_API_KEY);
	private static final Map<IChannel, String> CHANNELS_CUSTID = new HashMap<>();

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

		/*
				if(!GUILDS_CUSTID.containsKey(context.getGuild())) {
					GUILDS_CUSTID.put(context.getGuild(), null);
				}

				try {
					String aliceState = GUILDS_CUSTID.get(context.getGuild());
					String xmlString = NetUtils.getDoc("http://sheepridge.pandorabots.com/pandora/talk-xml?"
							+ "botid=b69b8d517e345aba"
							+ "&input=" + URLEncoder.encode(context.getArg(), "UTF-8")
							+ (aliceState == null ? "" : "&custid=" + aliceState)).toString();
					JSONObject resultObj = XML.toJSONObject(xmlString).getJSONObject("result");
					String response = resultObj.getString("that").replace("<br>", "\n").replace("  ", " ").trim();
					GUILDS_CUSTID.put(context.getChannel().getGuild(), resultObj.getString("custid"));
					BotUtils.sendMessage(Emoji.SPEECH + " " + response, context.getChannel());

				} catch (SocketTimeoutException e) {
					BotUtils.sendMessage(Emoji.HOURGLASS + " Sorry, A.L.I.C.E. is AFK, I'm sure she will be back very soon, try again later :)", context.getChannel());
					LogUtils.warn("SocketTimeoutException while chatting with A.L.I.C.E. (" + e.getMessage() + ").");

				} catch (IOException e) {
					if(e.getMessage().contains("502")) {
						BotUtils.sendMessage(Emoji.HOURGLASS + " Sorry, A.L.I.C.E. is AFK, I'm sure she will be back very soon, try again later :)", context.getChannel());
						LogUtils.warn("IOException while chatting with A.L.I.C.E. (" + e.getMessage() + ").");
					} else {
						LogUtils.error("Something went wrong while discussing with A.L.I.C.E.... Please, try again later.", e, context.getChannel());
					}
				}
		 */

		try {
			CleverBotQuery bot = new CleverBotQuery(API_KEY, context.getArg());
			bot.setConversationID(CHANNELS_CUSTID.get(context.getChannel()) == null ? "" : CHANNELS_CUSTID.get(context.getChannel()));
			bot.sendRequest();
			CHANNELS_CUSTID.put(context.getChannel(), bot.getConversationID());
			BotUtils.sendMessage(Emoji.SPEECH + " " + bot.getResponse(), context.getChannel());
		} catch (IOException e) {
			LogUtils.error("Something went wrong while discussing with Cleverbot... Please, try again later.", e, context.getChannel());
		}
	}

	@Override
	public void showHelp(Context context) {
		EmbedBuilder builder = new EmbedBuilder()
				.withAuthorName("Help for " + this.getNames()[0] + " command")
				.withAuthorIcon(Shadbot.getClient().getOurUser().getAvatarURL())
				.withColor(Config.BOT_COLOR)
				.appendDescription("**Chat with the artificial intelligence A.L.I.C.E.**")
				.appendField("Usage", context.getPrefix() + "chat <message>", false);
		BotUtils.sendEmbed(builder.build(), context.getChannel());
	}
}
