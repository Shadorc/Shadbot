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

	private static final Logger LOGGER = LoggerFactory.getLogger("Shadbot");

	public static void error(String input, IChannel channel, Throwable err, String msg) {
		LOGGER.error(String.format("%s (Input: %s)", msg, input), err);
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, msg, err, input, channel));
	}

	public static void error(Throwable err, String msg) {
		LOGGER.error(msg, err);
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, msg, err));
	}

	public static void error(String msg) {
		LOGGER.error(msg);
		LogUtils.sendLog(new LogBuilder(LogType.ERROR, msg));
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
