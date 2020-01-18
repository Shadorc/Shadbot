package com.shadorc.shadbot.utils;

import io.sentry.Sentry;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LogUtils {

    private enum TagName {
        EXCEPTION_CLASS("exception_class"),
        EXCEPTION_MESSAGE("exception_message"),
        TYPE("type");

        private final String name;

        TagName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum ExtraName {
        MESSAGE("message"),
        INPUT("input");

        private final String name;

        ExtraName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private enum TagValue {
        WARN("warn"),
        ERROR("error");

        private final String value;

        TagValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE.toString(), TagValue.ERROR.toString());
        Sentry.getContext().addTag(TagName.EXCEPTION_CLASS.toString(), error.getClass().getSimpleName());
        Sentry.getContext().addTag(TagName.EXCEPTION_MESSAGE.toString(), error.getMessage());
        Sentry.getContext().addExtra(ExtraName.MESSAGE.toString(), message);
        Sentry.getContext().addExtra(ExtraName.INPUT.toString(), input);
        Sentry.capture(error);
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE.toString(), TagValue.ERROR.toString());
        Sentry.getContext().addTag(TagName.EXCEPTION_CLASS.toString(), error.getClass().getSimpleName());
        Sentry.getContext().addTag(TagName.EXCEPTION_MESSAGE.toString(), error.getMessage());
        Sentry.getContext().addExtra(ExtraName.MESSAGE.toString(), message);
        Sentry.capture(error);
    }

    public static void error(String message) {
        LOGGER.error(message);

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE.toString(), TagValue.ERROR.toString());
        Sentry.capture(message);
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));

        Sentry.getContext().clear();
        Sentry.getContext().addTag(TagName.TYPE.toString(), TagValue.WARN.toString());
        Sentry.capture(String.format(format, args));
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

}
