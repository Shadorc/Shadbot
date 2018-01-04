package me.shadorc.shadbot.utils;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.common.collect.Lists;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class FormatUtils {

	public static <T> String formatList(String defaultStr, List<T> list, Function<T, String> mapper, String delimiter) {
		if(list.isEmpty()) {
			return defaultStr;
		}
		return list.stream().map(mapper).collect(Collectors.joining(delimiter)).toString();
	}

	public static <T> String formatList(List<T> list, Function<T, String> mapper, String delimiter) {
		return FormatUtils.formatList("", list, mapper, delimiter);
	}

	public static <T> String formatArray(T[] array, Function<T, String> mapper, String delimiter) {
		return FormatUtils.formatList(Arrays.asList(array), mapper, delimiter);
	}

	public static String formatNum(double num) {
		return NumberFormat.getNumberInstance(Locale.ENGLISH).format(num);
	}

	// TODO: Math.abs(coins) ?
	public static String formatCoins(int coins) {
		return String.format("%s coin%s", FormatUtils.formatNum(coins), Math.abs(coins) > 1 ? "s" : "");
	}

	public static String formatDate(Instant instant, ChronoUnit precision) {
		// [Years, Months, Days, Hours, Minutes, Seconds, Millis, Micros, Nanos]
		List<ChronoUnit> units = Lists.newArrayList(Arrays.stream(ChronoUnit.values())
				.limit(11)
				.filter(unit -> !unit.equals(ChronoUnit.HALF_DAYS) && !unit.equals(ChronoUnit.WEEKS))
				.collect(Collectors.toCollection(LinkedList::new))
				.descendingIterator());

		if(!units.contains(precision)) {
			throw new IllegalArgumentException(String.format("%s is not a valid precision.", precision));
		}

		Period period = Period.between(DateUtils.toLocalDate(instant).toLocalDate(), LocalDate.now());

		List<String> times = new ArrayList<>();
		for(ChronoUnit unit : units.stream().limit(units.indexOf(precision) + 1).collect(Collectors.toList())) {
			long time = period.get(unit);
			if(time != 0) {
				times.add(StringUtils.pluralOf(time, StringUtils.singularOf(unit.toString())));
			}
		}
		return FormatUtils.formatList(times, Object::toString, ", ");
	}

	public static String formatDuration(long durationMillis) {
		if(TimeUnit.MILLISECONDS.toHours(durationMillis) > 0) {
			return DurationFormatUtils.formatDuration(durationMillis, "H:mm:ss", true);
		}
		return DurationFormatUtils.formatDuration(durationMillis, "m:ss", true);
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
			strBuilder.append(" (" + FormatUtils.formatDuration(info.length) + ")");
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
