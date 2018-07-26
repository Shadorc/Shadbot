package me.shadorc.shadbot.utils.embed.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import discord4j.core.DiscordClient;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import me.shadorc.shadbot.Config;
import me.shadorc.shadbot.Shadbot;
import me.shadorc.shadbot.core.ExceptionHandler;
import me.shadorc.shadbot.utils.BotUtils;
import me.shadorc.shadbot.utils.FormatUtils;
import me.shadorc.shadbot.utils.embed.log.LogBuilder.LogType;

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

	public static void warn(DiscordClient client, String input, String msg) {
		LOGGER.warn(String.format("%s (Input: %s)", msg, input));
		LogUtils.sendLog(client, new LogBuilder(LogType.WARN, msg, null, input));
	}

	public static void warn(DiscordClient client, String msg) {
		LOGGER.warn(msg);
		LogUtils.sendLog(client, new LogBuilder(LogType.WARN, msg));
	}

	public static void infof(String format, Object... args) {
		LOGGER.info(String.format(format, args));
	}

	public static void cannot(Class<?> clazz, Snowflake guildId, Permission permission) {
		LogUtils.infof("{Guild ID: %d} {%s} Missing permission: %s",
				clazz.getSimpleName(), guildId.asLong(), FormatUtils.formatPermission(permission));
	}

	public static void cannot(Class<?> clazz, Permission permission) {
		LogUtils.infof("{%s} Missing permission: %s",
				clazz.getSimpleName(), FormatUtils.formatPermission(permission));
	}

	public static void cannotSpeak(Class<?> clazz, Snowflake guildId) {
		LogUtils.cannot(clazz, guildId, Permission.SEND_MESSAGES);
	}

	public static void cannotSpeak(Class<?> clazz) {
		LogUtils.cannot(clazz, Permission.SEND_MESSAGES);
	}

	private static void sendLog(DiscordClient client, LogBuilder embed) {
		BotUtils.sendMessage(embed.build(), client.getMessageChannelById(Config.LOGS_CHANNEL_ID))
				.doOnError(ExceptionHandler::isForbidden, error -> LogUtils.cannotSpeak(LogUtils.class))
				.subscribe();
	}

}
