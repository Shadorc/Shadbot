package com.shadorc.shadbot.utils;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
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

public final class FormatUtils {

    /**
     * @param coins The number of coins to format.
     * @return {@code X coin(s)} where {@code X} is the number of coins formatted using English locale.
     */
    public static String coins(long coins) {
        return String.format("%s coin%s", FormatUtils.number(coins), Math.abs(coins) > 1 ? "s" : "");
    }

    public static List<ImmutableEmbedFieldData> createColumns(List<String> list, int rowSize) {
        return Lists.partition(list, rowSize)
                .stream()
                .map(sublist -> ImmutableEmbedFieldData.of(
                        "\u200C",
                        String.join("\n", sublist),
                        Possible.of(true)))
                .collect(Collectors.toList());
    }

    /**
     * @param duration The duration to format.
     * @return The formatted duration as D days H hours S minutes.
     */
    public static String customDate(Duration duration) {
        final long minutes = duration.toMinutesPart();
        final long hours = duration.toHoursPart();
        final long days = duration.toDaysPart();

        final StringBuilder strBuilder = new StringBuilder();
        if (days > 0) {
            strBuilder.append(String.format("%s ", StringUtils.pluralOf(days, "day")));
        }
        if (hours > 0) {
            strBuilder.append(String.format("%s ", StringUtils.pluralOf(hours, "hour")));
        }
        if (minutes > 0 || days == 0 && hours == 0) {
            strBuilder.append(StringUtils.pluralOf(minutes, "minute"));
        }

        return strBuilder.toString().trim();
    }

    /**
     * @param instant The instant to format.
     * @return The formatted instant (e.g X days, Y hours, Z seconds).
     */
    public static String longDuration(Instant instant) {
        final Period period = Period.between(TimeUtils.toLocalDate(instant).toLocalDate(), LocalDate.now());
        final String str = period.getUnits().stream()
                .filter(unit -> period.get(unit) != 0)
                .map(unit -> String.format("%s %s", FormatUtils.number(period.get(unit)), unit.toString().toLowerCase()))
                .collect(Collectors.joining(", "));
        return str.isEmpty() ? FormatUtils.shortDuration(instant.toEpochMilli()) : str;
    }

    /**
     * @param durationMillis The duration to format (in milliseconds).
     * @return The formatted duration as H:mm:ss.
     */
    public static String shortDuration(long durationMillis) {
        if (TimeUnit.MILLISECONDS.toHours(durationMillis) > 0) {
            return DurationFormatUtils.formatDuration(durationMillis, "H:mm:ss", true);
        }
        return DurationFormatUtils.formatDuration(durationMillis, "m:ss", true);
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

    /**
     * @param number The double number to format.
     * @return The formatted number as a string using English locale.
     */
    public static String number(double number) {
        return NumberFormat.getNumberInstance(Locale.ENGLISH).format(number);
    }

    /**
     * @param limit The inclusive upper bound.
     * @param count The number of elements the stream should be limited to.
     * @param mapper A function to apply to each element.
     * @return A string consisting of the elements returned by the mapper applied to a stream of numbers from 1 to the minimum value
     * between {@code limit} and {@code count} and joined with new lines.
     */
    public static String numberedList(int limit, int count, Function<Integer, String> mapper) {
        return IntStream.rangeClosed(1, limit)
                .boxed()
                .limit(count)
                .map(mapper)
                .collect(Collectors.joining("\n"));
    }

    /**
     * @param enumClass The Enum class to get constants from.
     * @param <E> An enumeration.
     * @return {@code Options: `option_1`, `option_2`, ...}
     * @throws java.lang.IllegalArgumentException If the enumeration contains less than 2 constants.
     */
    public static <E extends Enum<E>> String options(Class<E> enumClass) {
        if (enumClass.getEnumConstants().length < 2) {
            throw new IllegalArgumentException("There must be at least two enum constants.");
        }
        return String.format("Options: %s",
                FormatUtils.format(enumClass.getEnumConstants(), value -> String.format("`%s`",
                        value.toString().toLowerCase()), ", "));
    }

    /**
     * @param info The {@link AudioTrackInfo} to format.
     * @return A string representing the provided info formatted.
     */
    public static String trackName(AudioTrackInfo info) {
        final StringBuilder strBuilder = new StringBuilder();
        if ("Unknown artist".equals(info.author)) {
            strBuilder.append(info.title.trim());
        } else {
            strBuilder.append(String.format("%s - %s", info.author.trim(), info.title.trim()));
        }

        if (info.isStream) {
            strBuilder.append(" (Stream)");
        } else {
            strBuilder.append(String.format(" (%s)", FormatUtils.shortDuration(info.length)));
        }

        return strBuilder.toString();
    }

}
