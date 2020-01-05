package com.shadorc.shadbot.utils;

import com.shadorc.shadbot.data.Config;
import com.shadorc.shadbot.object.LogBuilder;
import com.shadorc.shadbot.object.LogBuilder.LogType;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LogUtils {

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(GatewayDiscordClient client, Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);
        LogUtils.sendLog(client, LogBuilder.create(LogType.ERROR).setMessage(message).setError(error).setInput(input));
    }

    public static void error(GatewayDiscordClient client, Throwable error, String message) {
        LOGGER.error(message, error);
        LogUtils.sendLog(client, LogBuilder.create(LogType.ERROR).setMessage(message).setError(error));
    }

    public static void error(GatewayDiscordClient client, String message) {
        LOGGER.error(message);
        LogUtils.sendLog(client, LogBuilder.create(LogType.ERROR).setMessage(message));
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void warn(GatewayDiscordClient client, String message) {
        LOGGER.warn(message);
        LogUtils.sendLog(client, LogBuilder.create(LogType.WARN).setMessage(message));
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

    private static void sendLog(GatewayDiscordClient client, LogBuilder embed) {
        client.getChannelById(Config.LOGS_CHANNEL_ID)
                .cast(MessageChannel.class)
                .flatMap(channel -> DiscordUtils.sendMessage(embed.build(), channel))
                .subscribe(null, err -> ExceptionHandler.handleUnknownError(client, err));
    }

}
