package me.shadorc.discordbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {

	/**
	 * @param str - the String to capitalize
	 * @return str with the first letter capitalized
	 */
	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * @param count - the number
	 * @param word - the word to get plural from
	 * @return the plural of the word if necessary
	 */
	public static String pluralOf(long count, String word) {
		return count + " " + (count > 1 ? word + "s" : word);
	}

	/**
	 * @param arg - arg to split
	 * @param limit - the limit
	 * @return a String Array without null or empty string splitted by " "
	 */
	public static String[] getSplittedArg(String arg, int limit) {
		return Arrays.stream(arg.split(" ", limit)).filter(str -> str != null && !str.isEmpty()).toArray(String[]::new);
	}

	/**
	 * @param arg - arg to split
	 * @return a String Array without null or empty string splitted by " "
	 */
	public static String[] getSplittedArg(String arg) {
		return StringUtils.getSplittedArg(arg, 0);
	}

	/**
	 * @param text - the text to extract quoted words from
	 * @return List containing all non empty quoted words
	 */
	public static List<String> getQuotedWords(String text) {
		List<String> matches = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(text);
		while(matcher.find()) {
			matches.add(matcher.group(1));
		}
		matches.removeAll(Collections.singleton(""));
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
	 * @param min - the minimum
	 * @param max - the maximum
	 * @return true if str can be cast into a number between min (inclusive) and max (inclusive), false otherwise
	 */
	public static boolean isIntBetween(String str, int min, int max) {
		try {
			int num = Integer.parseInt(str);
			return num >= min && num <= max;
		} catch (NumberFormatException err) {
			return false;
		}
	}
}
