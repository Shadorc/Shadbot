package me.shadorc.shadbot.utils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class FormatUtils {

	public static <T> String format(Stream<T> stream, Function<T, String> mapper, String delimiter) {
		return stream.map(mapper).collect(Collectors.joining(delimiter));
	}

	public static <T> String format(List<T> list, Function<T, String> mapper, String delimiter) {
		return FormatUtils.format(list.stream(), mapper, delimiter);
	}

	public static <T> String format(T[] array, Function<T, String> mapper, String delimiter) {
		return FormatUtils.format(Arrays.stream(array), mapper, delimiter);
	}

	public static String formatNum(double num) {
		return NumberFormat.getNumberInstance(Locale.ENGLISH).format(num);
	}

	public static String formatCoins(int coins) {
		return String.format("%s coin%s", FormatUtils.formatNum(coins), Math.abs(coins) > 1 ? "s" : "");
	}

	public static String formatLongDuration(long duration) {
		Period period = Period.between(DateUtils.toLocalDate(Instant.ofEpochMilli(duration)).toLocalDate(), LocalDate.now());
		String str = period.getUnits().stream()
				.filter(unit -> period.get(unit) != 0)
				.map(unit -> String.format("%d %s", period.get(unit), unit.toString().toLowerCase()))
				.collect(Collectors.joining(", "));
		return str.isEmpty() ? FormatUtils.formatShortDuration(duration) : str;
	}

	public static String formatShortDuration(long duration) {
		if(TimeUnit.MILLISECONDS.toHours(duration) > 0) {
			return DurationFormatUtils.formatDuration(duration, "H:mm:ss", true);
		}
		return DurationFormatUtils.formatDuration(duration, "m:ss", true);
	}

	public static String formatTrackName(AudioTrackInfo info) {
		StringBuilder strBuilder = new StringBuilder();
		if("Unknown artist".equals(info.author)) {
			strBuilder.append(info.title);
		} else {
			strBuilder.append(info.author + " - " + info.title);
		}

		if(info.isStream) {
			strBuilder.append(" (Stream)");
		} else {
			strBuilder.append(" (" + FormatUtils.formatShortDuration(info.length) + ")");
		}

		return strBuilder.toString();
	}

	public static String numberedList(int count, int limit, Function<Integer, String> mapper) {
		return IntStream.rangeClosed(1, count)
				.boxed()
				.limit(limit)
				.map(mapper)
				.collect(Collectors.joining("\n"));
	}

}
