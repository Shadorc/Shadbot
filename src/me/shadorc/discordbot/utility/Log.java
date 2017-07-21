package me.shadorc.discordbot.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sx.blah.discord.handle.obj.IChannel;

public class Log {

	private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("[HH:mm:ss] ");
	private static final Logger LOGGER = LoggerFactory.getLogger("Logger");

	public static void print(String msg) {
		System.out.println(msg);
	}

	public static void info(String msg) {
		System.out.println(Log.getTime() + msg);
	}

	public static void error(String msg, Exception e, IChannel channel) {
		LOGGER.error(Log.getTime() + msg, e);
		BotUtils.sendMessage(msg, channel);
		e.printStackTrace();
	}

	public static void error(String msg, Exception e) {
		LOGGER.error(Log.getTime() + msg);
		e.printStackTrace();
	}

	public static void error(String msg) {
		LOGGER.error(Log.getTime() + msg);
	}

	private static String getTime() {
		return FORMATTER.format(new Date());
	}
}
