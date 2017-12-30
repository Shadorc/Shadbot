package me.shadorc.shadbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.shadbot.core.command.Context;
import me.shadorc.shadbot.utils.embed.LogBuilder;
import me.shadorc.shadbot.utils.embed.LogType;
import sx.blah.discord.handle.obj.IChannel;

public class LogUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger("Shadbot_Logger");
	// TODO: public static LogType type = LogType.WARN;

	private static void errorf(String message, Exception err, String input, IChannel channel) {
		BotUtils.sendMessage(new LogBuilder(LogType.ERROR, message, err, input, channel).build(), channel);
		if(input == null) {
			LOGGER.error(message, err);
		} else {
			LOGGER.error(String.format("%s (Input: %s)", message, input), err);
		}
	}

	public static void errorf(Context context, Exception err, String format, Object... args) {
		LogUtils.errorf(String.format(format, args), err, context.getContent(), context.getChannel());
	}

	public static void errorf(Exception err, String format, Object... args) {
		LogUtils.errorf(String.format(format, args), err, null, null);
	}

	public static void errorf(String format, Object... args) {
		LogUtils.errorf(String.format(format, args), null, null, null);
	}

	public static void error(String msg, Exception err) {
		LOGGER.error(msg, err);
	}

	public static void infof(String format, Object... args) {
		LOGGER.info(String.format(format, args));
	}

}
