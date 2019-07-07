package com.shadorc.shadbot.utils;

import reactor.util.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    /**
     * @param instant - the instant to get milliseconds from
     * @return The amount of milliseconds elapsed since {@code instant}
     */
    public static long getMillisUntil(@NonNull Instant instant) {
        return Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), TimeUtils.toLocalDate(instant)));
    }

    /**
     * @param epochMilli - the epoch milliseconds
     * @return The amount of milliseconds elapsed since {@code epochMillis}
     */
    public static long getMillisUntil(long epochMilli) {
        return TimeUtils.getMillisUntil(Instant.ofEpochMilli(epochMilli));
    }

    /**
     * Convert a string, case insensitive, representing time (example: 1m03s) into seconds. <br>
     * Supported units: s (second), m (minut), h (hour)
     *
     * @param str - the text to parse
     * @return The amount of seconds corresponding to the {@code str} format
     */
    public static long parseTime(@NonNull String str) {
        final String normalizedText = str.replaceAll(" ", "").toLowerCase();

        final Pattern pattern = Pattern.compile("[0-9]+[a-z]");
        final Matcher matcher = pattern.matcher(normalizedText);

        final List<String> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.group());
        }

        if (!String.join("", matches).equals(normalizedText)) {
            throw new IllegalArgumentException(str + " is not a valid time format.");
        }

        long seconds = 0;

        for (final String match : matches) {
            final long time = Long.parseLong(match.replaceAll("[a-z]", ""));
            final String unit = match.replaceAll("[0-9]", "");
            switch (unit) {
                case "s":
                    seconds += TimeUnit.SECONDS.toSeconds(time);
                    break;
                case "m":
                    seconds += TimeUnit.MINUTES.toSeconds(time);
                    break;
                case "h":
                    seconds += TimeUnit.HOURS.toSeconds(time);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown unit: " + unit);
            }
        }

        return seconds;
    }

    /**
     * @param instant - the instant to create the date-time from
     * @return {@code instant} converted as a {@link LocalDateTime} using the {@code ZoneId.systemDefault()} time-zone
     */
    public static LocalDateTime toLocalDate(@NonNull Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

}
