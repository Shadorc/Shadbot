package me.shadorc.shadbot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TimeUtils {

	/**
	 * @param instant - the instant to get milliseconds from
	 * @return The amount of milliseconds elapsed since {@code instant}
	 */
	public static long getMillisUntil(Instant instant) {
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
	 * @param instant - the instant to create the date-time from, not null
	 * @return {@code instant} converted as a {@link LocalDateTime} using the {@code ZoneId.systemDefault()} time-zone
	 */
	public static LocalDateTime toLocalDate(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	/**
	 * Convert a string, case insensitive, representing time (example: 1m03s) into seconds. <br>
	 * Supported units: s, m, h
	 * 
	 * @param str - the text to parse
	 * @return The amount of seconds corresponding to the {@code str} format
	 */
	public static long parseTime(String str) {
		String normalizedText = str.replaceAll(" ", "").toLowerCase();

		Pattern pattern = Pattern.compile("[0-9]+[a-z]{1}");
		Matcher matcher = pattern.matcher(normalizedText);

		List<String> matches = new ArrayList<>();
		while(matcher.find()) {
			matches.add(matcher.group());
		}

		if(!matches.stream().collect(Collectors.joining()).equals(normalizedText)) {
			throw new IllegalArgumentException(str + " is not a valid time format.");
		}

		long seconds = 0;

		for(String match : matches) {
			long time = Long.parseLong(match.replaceAll("[a-z]", ""));
			String unit = match.replaceAll("[0-9]", "");
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

}
