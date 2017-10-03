package me.shadorc.discordbot.utils;

import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class StringUtils {

	/**
	 * @param str - the String to capitalize
	 * @return str with the first letter capitalized
	 */
	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

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
		return StringUtils.formatList(Arrays.asList(array), mapper, delimiter);
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
			strBuilder.append(" (" + StringUtils.formatDuration(info.length) + ")");
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
	 * @return the formatted date as "d days h hours"
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

	public static String pluralOf(long count, String word) {
		return count + " " + (count > 1 ? word + "s" : word);
	}

	/**
	 * @param arg - arg to split
	 * @return a String Array without null or empty string splitted by " "
	 */
	public static String[] getSplittedArg(String arg) {
		return Arrays.stream(arg.split(" ")).filter(str -> str != null && !str.isEmpty()).toArray(String[]::new);
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast as a strictly positive Integer, false otherwise
	 */
	public static boolean isPositiveInt(String str) {
		try {
			return Integer.parseInt(str) > 0;
		} catch (NumberFormatException err) {
			return false;
		}
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast as a strictly positive Long, false otherwise
	 */
	public static boolean isPositiveLong(String str) {
		try {
			return Long.parseLong(str) > 0;
		} catch (NumberFormatException err) {
			return false;
		}
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast to a number between 1 and 6
	 */
	public static boolean isValidDiceNum(String str) {
		try {
			int num = Integer.parseInt(str);
			return num >= 1 && num <= 6;
		} catch (NumberFormatException err) {
			return false;
		}
	}

	/**
	 * @param text - the text to extract quoted words from
	 * @return List containing all quoted words
	 */
	public static List<String> getQuotedWords(String text) {
		List<String> matches = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(text);
		while(matcher.find()) {
			matches.add(matcher.group(1));
		}
		return matches;
	}

	/**
	 * @param str - the String to check
	 * @param charac - the Char to count
	 * @return Number of characters occurrences in str
	 */
	public static int getCharCount(String str, char charac) {
		int counter = 0;
		for(int i = 0; i < str.length(); i++) {
			if(str.charAt(i) == charac) {
				counter++;
			}
		}
		return counter;
	}
}
