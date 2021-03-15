package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.data.Config;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.util.annotation.Nullable;

import java.text.NumberFormat;
import java.time.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FormatUtil {

    /**
     * @param coins The number of coins to format.
     * @return {@code X coin(s)} where {@code X} is the number of coins formatted using English locale.
     */
    public static String coins(long coins) {
        return String.format("%s coin%s", FormatUtil.number(coins), Math.abs(coins) > 1 ? "s" : "");
    }

    /**
     * @param enumeration The enumeration to format, may be {@code null}.
     * @return The enumeration converted as a capitalized string with underscores replaced with spaces.
     */
    @Nullable
    public static <E extends Enum<E>> String capitalizeEnum(@Nullable E enumeration) {
        if (enumeration == null) {
            return null;
        }
        return StringUtil.capitalize(enumeration.toString().toLowerCase().replace("_", " "));
    }

    public static List<ImmutableEmbedFieldData> createColumns(List<String> list, int rowSize) {
        return ListUtil.partition(list, rowSize)
                .stream()
                .map(sublist -> ImmutableEmbedFieldData.of(
                        "\u200C",
                        String.join("\n", sublist),
                        Possible.of(true)))
                .collect(Collectors.toList());
    }

    /**
     * @param duration The duration to format.
     * @return The formatted duration as D day(s) H hour(s) S minute(s).
     */
    public static String formatDurationWords(Duration duration) {
        final long seconds = duration.toSecondsPart();
        final long minutes = duration.toMinutesPart();
        final long hours = duration.toHoursPart();
        final long days = duration.toDaysPart();

        final StringBuilder strBuilder = new StringBuilder();
        if (days > 0) {
            strBuilder.append(String.format("%s ", StringUtil.pluralOf(days, "day")));
        }
        if (hours > 0) {
            strBuilder.append(String.format("%s ", StringUtil.pluralOf(hours, "hour")));
        }
        if (minutes > 0) {
            strBuilder.append(String.format("%s ", StringUtil.pluralOf(minutes, "minute")));
        }
        if (seconds > 0 || days == 0 && hours == 0 && minutes == 0) {
            strBuilder.append(StringUtil.pluralOf(seconds, "second"));
        }

        return strBuilder.toString().trim();
    }

    /**
     * @param date The {@link LocalDateTime} to format.
     * @return The formatted instant (e.g Y year(s), M month(s), D day(s)).
     */
    public static String formatLongDuration(LocalDateTime date) {
        final Duration diff = Duration.between(date, LocalDateTime.now(ZoneId.systemDefault()));
        if (diff.toHours() < Duration.ofDays(1).toHours()) {
            return FormatUtil.formatDuration(diff);
        }

        final Period period = Period.between(date.toLocalDate(), LocalDate.now(ZoneId.systemDefault()));
        return period.getUnits().stream()
                .filter(unit -> period.get(unit) != 0)
                .map(unit -> StringUtil.pluralOf(period.get(unit), StringUtil.removeLastLetter(unit.toString().toLowerCase())))
                .collect(Collectors.joining(", "));
    }

    /**
     * @param millis The duration to format (in milliseconds).
     * @return The formatted duration as (H:)(mm:)ss.
     */
    public static String formatDuration(long millis) {
        return FormatUtil.formatDuration(Duration.ofMillis(millis));
    }

    /**
     * @param duration The duration to format.
     * @return The formatted duration as (H:)mm:ss.
     */
    public static String formatDuration(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must be positive");
        }
        if (duration.toHours() > 0) {
            return String.format("%d:%02d:%02d", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
        }
        return String.format("%d:%02d", duration.toMinutesPart(), duration.toSecondsPart());
    }

    public static <T extends Enum<T>> String format(Class<T> enumClass, CharSequence delimiter) {
        return FormatUtil.format(enumClass.getEnumConstants(), value -> value.name().toLowerCase(), delimiter);
    }

    public static <T> String format(T[] array, Function<T, String> mapper, CharSequence delimiter) {
        return FormatUtil.format(Arrays.stream(array), mapper, delimiter);
    }

    public static <T> String format(Collection<T> collection, Function<T, String> mapper, CharSequence delimiter) {
        return FormatUtil.format(collection.stream(), mapper, delimiter);
    }

    public static <T> String format(Stream<T> stream, Function<T, String> mapper, CharSequence delimiter) {
        return stream.map(mapper).collect(Collectors.joining(delimiter));
    }

    /**
     * @param number The double number to format.
     * @return The formatted number as a string using English locale.
     */
    public static String number(double number) {
        return NumberFormat.getNumberInstance(Config.DEFAULT_LOCALE).format(number);
    }

    /**
     * @param number The double number to format.
     * @param locale The desired locale.
     * @return The formatted number as a string using English locale.
     */
    public static String number(double number, Locale locale) {
        return NumberFormat.getNumberInstance(locale).format(number);
    }

    /**
     * @param limit  The inclusive upper bound.
     * @param count  The number of elements the stream should be limited to.
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
     * @param <E>       An enumeration.
     * @return {@code Options: `option_1`, `option_2`, ...}
     * @throws java.lang.IllegalArgumentException If the enumeration contains less than 2 constants.
     */
    public static <E extends Enum<E>> String options(Class<E> enumClass) {
        if (enumClass.getEnumConstants().length < 2) {
            throw new IllegalArgumentException("There must be at least two enum constants.");
        }
        return String.format("Options: %s",
                FormatUtil.format(enumClass.getEnumConstants(), value -> String.format("`%s`",
                        value.name().toLowerCase()), ", "));
    }

    /**
     * @param info The {@link AudioTrackInfo} to format.
     * @return A string representing the provided info formatted.
     */
    public static String trackName(AudioTrackInfo info) {
        final StringBuilder strBuilder = new StringBuilder();
        if (info.title == null) {
            strBuilder.append("Unknown video name");
        } else if ("Unknown artist".equals(info.author) || info.title.startsWith(info.author)) {
            strBuilder.append(info.title.trim());
        } else {
            strBuilder.append(String.format("%s - %s", info.author.trim(), info.title.trim()));
        }

        if (info.isStream) {
            strBuilder.append(" (Stream)");
        } else {
            strBuilder.append(String.format(" (%s)", FormatUtil.formatDuration(info.length)));
        }

        return strBuilder.toString();
    }

}
