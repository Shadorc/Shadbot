package com.shadorc.shadbot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.shadorc.shadbot.core.i18n.I18nManager;
import discord4j.discordjson.json.ImmutableEmbedFieldData;
import discord4j.discordjson.possible.Possible;
import reactor.util.annotation.Nullable;

import java.text.NumberFormat;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class FormatUtil {

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
     * @return The formatted duration as D day(s) H hour(s) M minute(s) or S second(s).
     */
    public static String formatDurationWords(Locale locale, Duration duration) {
        final long seconds = duration.toSecondsPart();
        final long minutes = duration.toMinutesPart();
        final long hours = duration.toHoursPart();
        final long days = duration.toDaysPart();

        final StringBuilder strBuilder = new StringBuilder();
        if (days > 0) {
            strBuilder.append(days == 1
                    ? I18nManager.localize(locale, "one.day")
                    : I18nManager.localize(locale, "several.days").formatted(I18nManager.localize(locale, days)))
                    .append(' ');
        }
        if (hours > 0) {
            strBuilder.append(hours == 1
                    ? I18nManager.localize(locale, "one.hour")
                    : I18nManager.localize(locale, "several.hours").formatted(hours))
                    .append(' ');
        }
        if (minutes > 0) {
            strBuilder.append(minutes == 1
                    ? I18nManager.localize(locale, "one.minute")
                    : I18nManager.localize(locale, "several.minutes").formatted(minutes))
                    .append(' ');
        }
        if (seconds > 0 || days == 0 && hours == 0 && minutes == 0) {
            strBuilder.append(seconds > 1
                    ? I18nManager.localize(locale, "several.seconds").formatted(seconds)
                    : I18nManager.localize(locale, "one.second").formatted(seconds)); // Can be 0 or 1 second
        }

        return strBuilder.toString().trim();
    }

    /**
     * @param date The {@link LocalDateTime} to format.
     * @return The formatted instant (e.g Y year(s), M month(s), D day(s)).
     */
    public static String formatLongDuration(Locale locale, LocalDateTime date) {
        final Duration diff = Duration.between(date, LocalDateTime.now(ZoneId.systemDefault()));
        if (diff.toHours() < Duration.ofDays(1).toHours()) {
            return FormatUtil.formatDuration(diff);
        }

        final Period period = Period.between(date.toLocalDate(), LocalDate.now(ZoneId.systemDefault()));
        final int years = period.getYears();
        final int months = period.getMonths();
        final int days = period.getDays();

        final List<String> units = new ArrayList<>();
        if (years > 0) {
            units.add(years == 1
                    ? I18nManager.localize(locale, "one.year")
                    : I18nManager.localize(locale, "several.years"));
        }
        if (months > 0) {
            units.add(months == 1
                    ? I18nManager.localize(locale, "one.month")
                    : I18nManager.localize(locale, "several.months"));
        }
        if (days > 0) {
            units.add(days == 1
                    ? I18nManager.localize(locale, "one.day")
                    : I18nManager.localize(locale, "several.days").formatted(days));
        }
        return String.join(", ", units);
    }

    /**
     * @param millis The duration to format (in milliseconds).
     * @return The formatted duration as (H:)mm:ss.
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
            throw new IllegalArgumentException("Duration must be positive");
        }
        if (duration.toHours() > 0) {
            return "%d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
        }
        return "%d:%02d".formatted(duration.toMinutesPart(), duration.toSecondsPart());
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
     * @param info The {@link AudioTrackInfo} to format.
     * @return A string representing the provided info formatted.
     */
    public static String trackName(Locale locale, AudioTrackInfo info) {
        final StringBuilder strBuilder = new StringBuilder();
        if (info.title == null) {
            strBuilder.append(I18nManager.localize(locale, "unknown.video.name"));
        } else if ("Unknown artist".equals(info.author) || info.title.startsWith(info.author)) {
            strBuilder.append(info.title.trim());
        } else {
            strBuilder.append("%s - %s".formatted(info.author.trim(), info.title.trim()));
        }

        if (info.isStream) {
            strBuilder.append(" (")
                    .append(I18nManager.localize(locale, "stream"))
                    .append(')');
        } else {
            strBuilder.append(" (")
                    .append(FormatUtil.formatDuration(info.length))
                    .append(')');
        }

        return strBuilder.toString();
    }

}
