package com.shadorc.shadbot.utils;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.common.json.EmbedFieldEntity;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FormatUtils {

    public static String coins(long coins) {
        return String.format("%s coin%s", FormatUtils.number(coins), Math.abs(coins) > 1 ? "s" : "");
    }

    public static List<EmbedFieldEntity> createColumns(List<String> list, int rowSize) {
        return Lists.partition(list, rowSize)
                .stream()
                .map(sublist -> new EmbedFieldEntity("\u200C", String.join("\n", sublist), true))
                .collect(Collectors.toList());
    }

    /**
     * @param duration - the duration to format
     * @return The formatted duration, not null, as D days and H hours and S minutes
     */
    public static String customDate(Duration duration) {
        final long minutes = duration.toMinutesPart();
        final long hours = duration.toHoursPart();
        final long days = duration.toDaysPart();
        return String.format("%s%s%s",
                days > 0 ? StringUtils.pluralOf(days, "day") + " " : "",
                hours > 0 ? StringUtils.pluralOf(hours, "hour") + " and " : "",
                StringUtils.pluralOf(minutes, "minute"));
    }

    public static <T extends Enum<T>> String format(Class<T> enumClass, CharSequence delimiter) {
        return FormatUtils.format(enumClass.getEnumConstants(), value -> value.toString().toLowerCase(), delimiter);
    }

    public static <T> String format(Collection<T> collection, Function<T, String> mapper, CharSequence delimiter) {
        return FormatUtils.format(collection.stream(), mapper, delimiter);
    }

    public static <T> String format(Stream<T> stream, Function<T, String> mapper, CharSequence delimiter) {
        return stream.map(mapper).collect(Collectors.joining(delimiter));
    }

    public static <T> String format(T[] array, Function<T, String> mapper, CharSequence delimiter) {
        return FormatUtils.format(Arrays.stream(array), mapper, delimiter);
    }

    public static String longDuration(Instant instant) {
        final Period period = Period.between(TimeUtils.toLocalDate(instant).toLocalDate(), LocalDate.now());
        final String str = period.getUnits().stream()
                .filter(unit -> period.get(unit) != 0)
                .map(unit -> String.format("%d %s", period.get(unit), unit.toString().toLowerCase()))
                .collect(Collectors.joining(", "));
        return str.isEmpty() ? FormatUtils.shortDuration(instant.toEpochMilli()) : str;
    }

    /**
     * @param number the double number to format
     * @return the formatted String using English locale
     */
    public static String number(double number) {
        return NumberFormat.getNumberInstance(Locale.ENGLISH).format(number);
    }

    public static String numberedList(int count, int limit, Function<Integer, String> mapper) {
        return IntStream.rangeClosed(1, count)
                .boxed()
                .limit(limit)
                .map(mapper)
                .collect(Collectors.joining("\n"));
    }

    public static <E extends Enum<E>> String options(Class<E> enumClass) {
        return String.format("Options: %s",
                FormatUtils.format(enumClass.getEnumConstants(), value -> String.format("`%s`", value.toString().toLowerCase()), ", "));
    }

    /**
     * @param durationMillis - the duration to format in milliseconds
     * @return The formatted duration, not null, as H:mm:ss
     */
    public static String shortDuration(long durationMillis) {
        if (TimeUnit.MILLISECONDS.toHours(durationMillis) > 0) {
            return DurationFormatUtils.formatDuration(durationMillis, "H:mm:ss", true);
        }
        return DurationFormatUtils.formatDuration(durationMillis, "m:ss", true);
    }

    public static String trackName(AudioTrackInfo info) {
        final StringBuilder strBuilder = new StringBuilder();
        if ("Unknown artist".equals(info.author)) {
            strBuilder.append(info.title);
        } else {
            strBuilder.append(String.format("%s - %s", info.author, info.title));
        }

        if (info.isStream) {
            strBuilder.append(" (Stream)");
        } else {
            strBuilder.append(String.format(" (%s)", FormatUtils.shortDuration(info.length)));
        }

        return String.format("[%s](%s)", strBuilder.toString(), info.uri);
    }

}
