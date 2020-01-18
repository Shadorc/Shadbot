package com.shadorc.shadbot.utils;

import io.sentry.Sentry;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LogUtils {

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag("type", "error");
        Sentry.getContext().addExtra("message", message);
        Sentry.getContext().addExtra("input", input);
        Sentry.capture(error);
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag("type", "error");
        Sentry.getContext().addExtra("message", message);
        Sentry.capture(error);
    }

    public static void error(String message) {
        LOGGER.error(message);

        Sentry.getContext().clear();
        Sentry.getContext().addTag("type", "error");
        Sentry.capture(message);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));

        Sentry.getContext().clear();
        Sentry.getContext().addTag("type", "warn");
        Sentry.capture(String.format(format, args));
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

}
