package me.shadorc.shadbot.utils;

import java.text.NumberFormat;
import java.util.ArrayList;
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

	public static String formatDuration(long duration) {
		long totalSecs = duration / 1000;
		long days = totalSecs / (24 * 60 * 60);
		long hours = totalSecs / (60 * 60) % 24;
		long mins = (totalSecs / 60) % 60;

		List<String> test = new ArrayList<>();
		if(days != 0) {
			test.add(StringUtils.pluralOf(days, "day"));
		}
		if(hours != 0) {
			test.add(StringUtils.pluralOf(hours, "hour"));
		}
		if(mins != 0) {
			test.add(StringUtils.pluralOf(mins, "minute"));
		}
		return format(test, Object::toString, ", ");
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
