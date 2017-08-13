package me.shadorc.discordbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.discordbot.utils.BotUtils;
import sx.blah.discord.handle.obj.IChannel;

public class Log {

	private static final Logger LOGGER = LoggerFactory.getLogger("Logger");

	public static void error(String msg, Exception e, IChannel channel) {
		LOGGER.error(msg, e);
		BotUtils.sendMessage(Emoji.ERROR + " " + msg, channel);
		Log.sendMessage("An error occured and users have been warned: " + msg + "\nSee logs for more info.", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void error(String msg, Exception e) {
		LOGGER.error(msg, e);
		Log.sendMessage("An error occured: " + msg + "\nSee logs for more info.", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void error(String msg) {
		LOGGER.error(msg);
		Log.sendMessage("An error occured: " + msg + "\nSee logs for more info.", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void warn(String msg) {
		LOGGER.warn(msg);
	}

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	private static void sendMessage(String msg, long channelId) {
		BotUtils.sendMessage(msg, Shadbot.getClient().getChannelByID(channelId));
	}
}
