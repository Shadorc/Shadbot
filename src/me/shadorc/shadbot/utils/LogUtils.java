package me.shadorc.shadbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.discordbot.data.Config;
import me.shadorc.shadbot.Shadbot;

public class LogUtils {

	public enum LogType {
		INFO, WARN, ERROR;
	}

	private static final Logger LOGGER = LoggerFactory.getLogger("Shadbot_Logger");

	public static LogType type;

	public static void error(String msg, Exception err) {
		if(type.equals(LogType.ERROR)) {
			LogUtils.sendLog(new LogBuilder(LogType.ERROR, msg, err));
		}
		LOGGER.error(msg, err);
	}

	public static void error(String msg) {
		LogUtils.error(msg, null);
	}

	public static void errorf(String format, Object... args) {
		LogUtils.error(String.format(format, args));
	}

	public static void warn(String msg) {
		if(type.equals(LogType.WARN)) {
			LogUtils.sendLog(new LogBuilder(LogType.WARN, msg));
		}
		LOGGER.warn(msg);
	}

	public static void info(String msg) {
		if(type.equals(LogType.INFO)) {
			LogUtils.sendLog(new LogBuilder(LogType.INFO, msg));
		}
		LOGGER.info(msg);
	}

	private static void sendLog(LogBuilder logBuilder) {
		BotUtils.sendMessage(logBuilder.build(), Shadbot.getClient().getChannelByID(Config.LOGS_CHANNEL_ID));
	}
}
