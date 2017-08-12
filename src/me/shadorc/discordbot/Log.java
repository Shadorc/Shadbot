package me.shadorc.discordbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IChannel;

public class Log {

	private static final Logger LOGGER = LoggerFactory.getLogger("Logger");

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	public static void error(String msg, Exception e, IChannel channel) {
		LOGGER.error(msg, e);
		BotUtils.sendMessage(Emoji.ERROR + " " + msg, channel);
	}

	public static void error(String msg, Exception e) {
		LOGGER.error(msg);
		e.printStackTrace();
	}

	public static void warn(String msg) {
		LOGGER.warn(msg);
	}
}
