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

	/** Return the amount of milliseconds elapsed since {@code instant} */
	public static long getMillisUntil(Instant instant) {
		return Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), TimeUtils.toLocalDate(instant)));
	}

	/** Return the amount of milliseconds elapsed since {@code epochMilli} */
	public static long getMillisUntil(long epochMilli) {
		return TimeUtils.getMillisUntil(Instant.ofEpochMilli(epochMilli));
	}

	/** Convert {@code instant} to {@link LocalDateTime} */
	public static LocalDateTime toLocalDate(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	/**
	 * Convert a String {@code text} representing time (example: 1m03s) into seconds. <br>
	 * Case insensitive. <br>
	 * Supported units: s, m, h
	 */
	public static long parseTime(final String text) {
		String normalizedText = text.replaceAll(" ", "").toLowerCase();

		Pattern pattern = Pattern.compile("[0-9]+[a-z]{1}");
		Matcher matcher = pattern.matcher(normalizedText);

		List<String> list = new ArrayList<>();
		while(matcher.find()) {
			list.add(matcher.group());
		}

		if(!list.stream().collect(Collectors.joining()).equals(normalizedText)) {
			throw new IllegalArgumentException(text + " is not a valid time format.");
		}

		long totalMs = 0;

		for(String str : list) {
			long time = Long.parseLong(str.replaceAll("[a-z]", ""));
			String unit = str.replaceAll("[0-9]", "");
			switch (unit) {
				case "s":
					totalMs += TimeUnit.SECONDS.toSeconds(time);
					break;
				case "m":
					totalMs += TimeUnit.MINUTES.toSeconds(time);
					break;
				case "h":
					totalMs += TimeUnit.HOURS.toSeconds(time);
					break;
				default:
					throw new IllegalArgumentException("Unknown unit: " + unit);
			}
		}

		return totalMs;
	}

}
