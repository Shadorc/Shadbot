package com.shadorc.shadbot.utils;

import reactor.util.Logger;
import reactor.util.Loggers;

public class LogUtils {

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

    public static void debug(String format, Object... args) {
        LOGGER.debug(String.format(format, args));
    }

}
