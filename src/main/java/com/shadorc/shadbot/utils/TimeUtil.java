package com.shadorc.shadbot.utils;

import reactor.util.annotation.NonNull;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtil {

    private static final Pattern LETTER_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");

    /**
     * @param instant The instant
     * @return The duration elapsed since {@code instant}.
     */
    public static Duration elapsed(@NonNull Instant instant) {
        return Duration.ofMillis(Math.abs(ChronoUnit.MILLIS.between(Instant.now(), instant)));
    }

    /**
     * @param epochMilli The epoch milliseconds.
     * @return The duration elapsed since {@code epochMillis}.
     */
    public static Duration elapsed(long epochMilli) {
        return TimeUtil.elapsed(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * Convert a string, case insensitive, representing time (e.g. 1m03s) or seconds (e.g. 72) into duration.<br>
     * Supported units: s (second), m (minute), h (hour)
     *
     * @param str The text to parse.
     * @return The duration corresponding to the {@code str} format.
     */
    public static Duration parseTime(@NonNull String str) {
        // If the argument is a number of seconds...
        final Long secondsValue = NumberUtil.toPositiveLongOrNull(str);
        if (secondsValue != null) {
            return Duration.ofSeconds(secondsValue);
        }

        final String normalizedText = str.replace(" ", "").toLowerCase();

        final Pattern pattern = Pattern.compile("[0-9]+[a-z]?");
        final Matcher matcher = pattern.matcher(normalizedText);

        final List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        if (!String.join("", matches).equals(normalizedText)) {
            throw new IllegalArgumentException("%s is not a valid time format.".formatted(str));
        }

        long seconds = 0;

        for (final String match : matches) {
            final long time = Long.parseLong(LETTER_PATTERN.matcher(match).replaceAll(""));
            String unit = NUMBER_PATTERN.matcher(match).replaceAll("");
            if(unit.isBlank()) {
                unit = "s";
            }
            switch (unit) {
                case "s" -> seconds += TimeUnit.SECONDS.toSeconds(time);
                case "m" -> seconds += TimeUnit.MINUTES.toSeconds(time);
                case "h" -> seconds += TimeUnit.HOURS.toSeconds(time);
                default -> throw new IllegalArgumentException("Unknown unit: %s".formatted(unit));
            }
        }

        return Duration.ofSeconds(seconds);
    }

    /**
     * @param instant The instant to create the date-time from.
     * @return {@code instant} converted as a {@link LocalDateTime} using the {@code ZoneId.systemDefault()} time-zone.
     */
    public static LocalDateTime toLocalDateTime(@NonNull Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
