package com.shadorc.shadbot.utils;

import io.sentry.Sentry;
import io.sentry.event.Event;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import reactor.util.Logger;
import reactor.util.Loggers;

public final class LogUtils {

    private static final class TagName {

        public static final String EXCEPTION_CLASS = "exception_class";
        public static final String EXCEPTION_MESSAGE = "exception_message";
    }

    private static final class ExtraName {

        public static final String INPUT = "input";
    }

    private static final Logger LOGGER = Loggers.getLogger("shadbot");

    public static void error(Throwable error, String message, String input) {
        LOGGER.error(String.format("%s (Input: %s)", message, input), error);

        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(error))
                .withMessage(message)
                .withTag(TagName.EXCEPTION_CLASS, error.getClass().getSimpleName())
                .withTag(TagName.EXCEPTION_MESSAGE, error.getMessage())
                .withExtra(ExtraName.INPUT, input)
                .build());
    }

    public static void error(Throwable error, String message) {
        LOGGER.error(message, error);

        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.ERROR)
                .withSentryInterface(new ExceptionInterface(error))
                .withMessage(message)
                .withTag(TagName.EXCEPTION_CLASS, error.getClass().getSimpleName())
                .withTag(TagName.EXCEPTION_MESSAGE, error.getMessage())
                .build());
    }

    public static void error(String message) {
        LOGGER.error(message);

        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.ERROR)
                .withMessage(message)
                .build());
    }

    public static void warn(String format, Object... args) {
        LOGGER.warn(String.format(format, args));

        Sentry.capture(new EventBuilder()
                .withLevel(Event.Level.WARNING)
                .withMessage(String.format(format, args))
                .build());
    }

    public static void info(String format, Object... args) {
        LOGGER.info(String.format(format, args));
    }

}
