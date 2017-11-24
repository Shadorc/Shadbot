package me.shadorc.discordbot.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.shadorc.discordbot.Shadbot;
import me.shadorc.discordbot.command.Context;
import me.shadorc.discordbot.data.Config;
import me.shadorc.discordbot.utils.command.Emoji;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.EmbedBuilder;

public class LogUtils {

	public static final Logger LOGGER = LoggerFactory.getLogger("Logger");

	private enum LogType {
		WARN, ERROR;
	}

	/**
	 * @param msg - the custom error message
	 * @param err - the exception
	 * @param input - the user's input
	 * @param cmdName - the name of the command executed while the exception occurred
	 * @param channel - the channel in which send a message to warn users of this error
	 */
	private static void error(String msg, Exception err, String input, String cmdName, IChannel channel) {
		if(channel != null) {
			BotUtils.sendMessage(Emoji.RED_FLAG + " " + msg, channel);
		}

		LOGGER.error(msg + (input == null ? "" : " (Input: " + input + ")"), err);
		LogUtils.sendEmbedLog(LogType.ERROR, msg,
				"Command", cmdName,
				"Error message", (err == null ? null : err.getMessage()),
				"Input", input,
				"User warned", Boolean.toString(channel != null));
	}

	public static void error(String msg, Exception err, Context context) {
		LogUtils.error(msg, err, context.getMessage().getContent(), context.getCommand(), context.getChannel());
	}

	public static void error(String msg, Exception err, String input) {
		LogUtils.error(msg, err, input, null, null);
	}

	public static void error(String msg, Exception err) {
		LogUtils.error(msg, err, null, null, null);
	}

	public static void error(String msg) {
		LogUtils.error(msg, null, null, null, null);
	}

	public static void warn(String msg) {
		LOGGER.warn(msg);
		LogUtils.sendEmbedLog(LogType.WARN, msg);
	}

	public static void info(String msg) {
		LOGGER.info(msg);
	}

	private static void sendEmbedLog(LogType type, String msg, String... fields) {
		EmbedBuilder builder = Utils.getDefaultEmbed()
				.setLenient(true)
				.withAuthorName(StringUtils.capitalize(type.toString().toLowerCase()) + " (Version: " + Config.VERSION.toString() + ")")
				.withDescription(msg);

		for(int i = 0; i < fields.length; i += 2) {
			builder.appendField(fields[i], fields[i + 1], true);
		}

		BotUtils.sendMessage(builder.build(), Shadbot.getClient().getChannelByID(Config.LOGS_CHANNEL_ID));
	}
}
