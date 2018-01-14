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

public class TimeUtils {

	public static long getMillisUntil(Instant instant) {
		return Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), TimeUtils.toLocalDate(instant)));
	}

	public static long getMillisUntil(long epochMilli) {
		return TimeUtils.getMillisUntil(Instant.ofEpochMilli(epochMilli));
	}

	public static LocalDateTime toLocalDate(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

	public static long parseTime(String text) {
		Pattern pattern = Pattern.compile("[^a-z]*[a-z]{1}");
		Matcher matcher = pattern.matcher(text.toLowerCase());

		List<String> list = new ArrayList<>();
		while(matcher.find()) {
			list.add(matcher.group());
		}

		if(list.isEmpty() || list.stream().mapToInt(String::length).sum() != text.length()) {
			throw new IllegalArgumentException("Unit is missing");
		}

		long totalMs = 0;

		for(String str : list) {
			String unit = str.replaceAll("[0-9]", "");
			try {
				int time = Integer.parseInt(str.replaceAll("[a-zA-Z]", ""));
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
			} catch (NumberFormatException err) {
				throw new IllegalArgumentException(String.format("Missing number for unit \"%s\"", unit));
			}
		}

		return totalMs;
	}

}
