package me.shadorc.discordbot.utils;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class FormatUtils {

	/**
	 * @param list - the list to format
	 * @param mapper - a non-interfering, stateless function to apply to each element
	 * @param delimiter - the delimiter to be used between each element
	 * @return formatted list
	 */
	public static <T> String formatList(List<T> list, Function<T, String> mapper, String delimiter) {
		return list.stream().map(mapper).collect(Collectors.joining(delimiter)).toString();
	}

	/**
	 * @param array - the array to format
	 * @param mapper - a non-interfering, stateless function to apply to each element
	 * @param delimiter - the delimiter to be used between each element
	 * @return formatted array
	 */
	public static String formatArray(Object[] array, Function<Object, String> mapper, String delimiter) {
		return FormatUtils.formatList(Arrays.asList(array), mapper, delimiter);
	}

	/**
	 * @param info - the info from the audio track to format
	 * @return formatted name
	 */
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

	/**
	 * @param duration - the duration to format
	 * @return the formatted duration as "m:ss" or "H:mm:ss" if duration is longer than an hour
	 */
	public static String formatDuration(long durationMillis) {
		if(TimeUnit.MILLISECONDS.toHours(durationMillis) > 0) {
			return DurationFormatUtils.formatDuration(durationMillis, "H:mm:ss", true);
		}
		return DurationFormatUtils.formatDuration(durationMillis, "m:ss", true);
	}

	/**
	 * @param date - the date to format
	 * @return the formatted date as "y years m months d days"
	 */
	public static String formateDate(LocalDateTime date) {
		Period period = Period.between(date.toLocalDate(), LocalDateTime.now().toLocalDate());
		long years = period.get(ChronoUnit.YEARS);
		long months = period.get(ChronoUnit.MONTHS);
		long days = period.get(ChronoUnit.DAYS);

		StringBuilder strBuilder = new StringBuilder();
		if(years != 0) {
			strBuilder.append(StringUtils.pluralOf(years, "year") + ", ");
		}
		if(months != 0) {
			strBuilder.append(StringUtils.pluralOf(months, "month") + ", ");
		}
		strBuilder.append(StringUtils.pluralOf(days, "day"));

		return strBuilder.toString();
	}

	/**
	 * @param num - the double number to format
	 * @return the formatted String using English locale
	 */
	public static String formatNum(double num) {
		return NumberFormat.getNumberInstance(Locale.ENGLISH).format(num);
	}

	/**
	 * @param coins - number of coins to format
	 * @return coins formatted + "coin(s)"
	 */
	public static String formatCoins(int coins) {
		return FormatUtils.formatNum(coins) + " " + (coins > 1 ? "coins" : "coin");
	}
}
