package me.shadorc.shadbot.utils;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

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

	public static String formatArray(Object[] array, Function<Object, String> mapper, String delimiter) {
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
		// [Years, Months, Weeks, Days, Hours, Minutes, Seconds, Millis, Micros, Nanos]
		List<ChronoUnit> units = Lists.newArrayList(Arrays.stream(ChronoUnit.values())
				.limit(11)
				.filter(unit -> !unit.equals(ChronoUnit.HALF_DAYS))
				.collect(Collectors.toCollection(LinkedList::new))
				.descendingIterator());

		if(!units.contains(precision)) {
			throw new IllegalArgumentException(String.format("%s is not a valid precision.", precision));
		}

		Duration duration = Duration.ofMillis(DateUtils.getMillisUntil(instant));

		StringBuilder strBuilder = new StringBuilder();
		for(ChronoUnit unit : units.stream().limit(units.indexOf(precision)).collect(Collectors.toList())) {
			long time = duration.get(unit);
			if(time != 0) {
				strBuilder.append(StringUtils.pluralOf(time, StringUtils.singularOf(unit.toString())) + ", ");
			}
		}
		return strBuilder.toString();
	}

}
