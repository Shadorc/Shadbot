package com.shadorc.shadbot.utils;

import io.sentry.Sentry;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LogUtils {

    private static final class TagName {

        public static final String EXCEPTION_CLASS = "exception_class";
        public static final String EXCEPTION_MESSAGE = "exception_message";
        public static final String TYPE = "type";
    }

    private static final class ExtraName {

        public static final String MESSAGE = "message";
        public static final String INPUT = "input";
    }

    private static final class TagValue {

        public static final String WARN = "warn";
        public static final String ERROR = "error";
    }

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE, TagValue.ERROR);
        Sentry.getContext().addTag(TagName.EXCEPTION_CLASS, error.getClass().getSimpleName());
        Sentry.getContext().addTag(TagName.EXCEPTION_MESSAGE, error.getMessage());
        Sentry.getContext().addExtra(ExtraName.MESSAGE, message);
        Sentry.getContext().addExtra(ExtraName.INPUT, input);
        Sentry.capture(error);
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE, TagValue.ERROR);
        Sentry.getContext().addTag(TagName.EXCEPTION_CLASS, error.getClass().getSimpleName());
        Sentry.getContext().addTag(TagName.EXCEPTION_MESSAGE, error.getMessage());
        Sentry.getContext().addExtra(ExtraName.MESSAGE, message);
        Sentry.capture(error);
    }

    public static void error(String message) {
        LOGGER.error(message);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE, TagValue.ERROR);
        Sentry.capture(message);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE, TagValue.WARN);
        Sentry.capture(String.format(format, args));
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

}
