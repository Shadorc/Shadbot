package me.shadorc.shadbot.utils.embed.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.core.DiscordClient;
import discord4j.core.object.entity.MessageChannel;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.utils.DiscordUtils;
import me.shadorc.shadbot.utils.embed.log.LogBuilder.LogType;
import me.shadorc.shadbot.utils.exception.ExceptionHandler;

public class LogUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Shadbot.class);

	public static void error(DiscordClient client, Throwable err, String msg, String input) {
		LOGGER.error(String.format("%s (Input: %s)", msg, input), err);
		LogUtils.sendLog(client, new LogBuilder(LogType.ERROR, msg, err, input));
	}

	public static void error(DiscordClient client, Throwable err, String msg) {
		LOGGER.error(msg, err);
		LogUtils.sendLog(client, new LogBuilder(LogType.ERROR, msg, err));
	}

	public static void error(DiscordClient client, String msg) {
		LOGGER.error(msg);
		LogUtils.sendLog(client, new LogBuilder(LogType.ERROR, msg));
	}

	public static void error(Throwable err, String msg) {
		LOGGER.error(msg, err);
	}

	public static void error(String msg) {
		LOGGER.error(msg);
	}

	public static void warn(DiscordClient client, String msg, String input) {
		LOGGER.warn(String.format("%s (Input: %s)", msg, input));
		LogUtils.sendLog(client, new LogBuilder(LogType.WARN, msg, null, input));
	}

	public static void warn(DiscordClient client, String msg) {
		LOGGER.warn(msg);
		LogUtils.sendLog(client, new LogBuilder(LogType.WARN, msg));
	}

	public static void info(String format, Object... args) {
		LOGGER.info(String.format(format, args));
	}

	private static void sendLog(DiscordClient client, LogBuilder embed) {
		client.getChannelById(Config.LOGS_CHANNEL_ID)
				.cast(MessageChannel.class)
				.flatMap(channel -> DiscordUtils.sendMessage(embed.build(), channel))
				.onErrorResume(thr -> ExceptionHandler.handleUnknownError(thr, client))
				.subscribe();
	}

}
