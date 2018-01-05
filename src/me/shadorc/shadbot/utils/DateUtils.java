package me.shadorc.shadbot.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class DateUtils {

	public static long getMillisUntil(Instant instant) {
		return Math.abs(ChronoUnit.MILLIS.between(LocalDateTime.now(), DateUtils.toLocalDate(instant)));
	}

	public static long getMillisUntil(long epochMilli) {
		return DateUtils.getMillisUntil(Instant.ofEpochMilli(epochMilli));
	}

	public static LocalDateTime toLocalDate(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
	}

}
