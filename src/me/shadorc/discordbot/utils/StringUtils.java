package me.shadorc.discordbot.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
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
	 * @param text - the String to convert, may be null
	 * @return a new converted String, null if null string input
	 */
	public static String convertHtmlToUTF8(String text) {
		return StringEscapeUtils.unescapeHtml3(text);
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
			strBuilder.append(" (" + StringUtils.formatDuration(info.length) + ")");
		}

		return strBuilder.toString();
	}

	/**
	 * @param duration - the duration to format
	 * @return the formatted duration
	 */
	public static String formatDuration(long duration) {
		return DurationFormatUtils.formatDuration(duration, "m:ss", true);
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast as an Integer, false otherwise
	 */
	public static boolean isInteger(String str) {
		try {
			Integer.parseInt(str);
			return true;
		} catch (NumberFormatException err) {
			return false;
		}
	}

	/**
	 * @param str - the String to check
	 * @return true if it can be cast as a strictly positive Integer, false otherwise
	 */
	public static boolean isPositiveInteger(String str) {
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
	 * @param list - the list to format
	 * @param mapper - a non-interfering, stateless function to apply to each element
	 * @param delimiter - the delimiter to be used between each element
	 * @return formatted list
	 */
	public static <T> String formatList(List<T> list, Function<T, String> mapper, String delimiter) {
		return list.stream().map(mapper).collect(Collectors.joining(delimiter)).toString();
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
	 * @param str - String to check
	 * @param charac - Char to count
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
