package me.shadorc.discordbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.discordbot.Config;
import me.shadorc.discordbot.Emoji;
import me.shadorc.discordbot.Shadbot;
import sx.blah.discord.handle.obj.IChannel;

public class LogUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger("Logger");

	public static void error(String msg, Exception err, IChannel channel) {
		LOGGER.error(msg, err);
		BotUtils.sendMessage(Emoji.RED_FLAG + " " + msg, channel);
		LogUtils.sendMessage("**[ERROR]** An error occured and users have been warned: *" + msg + "* (Error message: " + err.getMessage() + ")", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void error(String msg, IChannel channel) {
		LOGGER.error(msg);
		BotUtils.sendMessage(Emoji.RED_FLAG + " " + msg, channel);
		LogUtils.sendMessage("**[ERROR]** An error occured and users have been warned: *" + msg + "* (Message: " + msg + ")", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void error(String msg, Exception err) {
		LOGGER.error(msg, err);
		LogUtils.sendMessage("**[ERROR]** An error occured: *" + msg + "* (Error message: " + err.getMessage() + ")", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void error(String msg) {
		LOGGER.error(msg);
		LogUtils.sendMessage("**[ERROR]** An error occured: *" + msg + "*", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void warn(String msg) {
		LOGGER.warn(msg);
		LogUtils.sendMessage("**[WARN]** *" + msg + "*", Config.BUGS_REPORT_CHANNEL_ID);
	}

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	private static void sendMessage(String msg, long channelId) {
		BotUtils.sendMessage(msg, Shadbot.getClient().getChannelByID(channelId));
	}
}
