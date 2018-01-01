package me.shadorc.shadbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.embed.LogBuilder;
import me.shadorc.shadbot.utils.embed.LogType;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.obj.IChannel;

public class LogUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger("Shadbot-Logger");

	public static void errorf(String input, IChannel channel, Exception err, String format, Object... args) {
		LOGGER.error(String.format(format, args), err);
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, String.format(format, args), err, input, channel));
		BotUtils.sendMessage(String.format(format, args), channel);
	}

	public static void errorf(Exception err, String format, Object... args) {
		LOGGER.error(String.format(format, args), err);
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, String.format(format, args), err));
	}

	public static void errorf(String format, Object... args) {
		LOGGER.error(String.format(format, args));
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, String.format(format, args)));
	}

	public static void warnf(String format, Object... args) {
		LOGGER.warn(String.format(format, args));
		LogUtils.sendLog(new LogBuilder(LogType.WARN, String.format(format, args)));
	}

	public static void infof(String format, Object... args) {
		LOGGER.info(String.format(format, args));
	}

	private static void sendLog(LogBuilder embed) {
		IDiscordClient client = Shadbot.getClient();
		if(client != null && client.isLoggedIn()) {
			BotUtils.sendMessage(embed.build(), client.getChannelByID(Config.LOGS_CHANNEL_ID));
		}
	}

}
