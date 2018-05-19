package me.shadorc.shadbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

	/**
	 * @param str - the string to split
	 * @param limit - the result threshold
	 * @param delimiter - the delimiting regular expression
	 * @return A list with a maximum number of {@code limit} elements containing all the results of {@code str} splitted using {@code delimiter} excluding
	 *         empty results
	 */
	public static List<String> split(String str, int limit, String delimiter) {
		return Arrays.stream(str.split(delimiter, limit))
				.map(String::trim)
				.filter(word -> !word.isEmpty())
				.collect(Collectors.toList());
	}

	/**
	 * @param str - the string to split
	 * @param limit - the result threshold
	 * @param delimiter - the delimiting regular expression
	 * @return A endless list containing all the elements resulting of {@code str} splitted using space excluding empty results
	 */
	public static List<String> split(String str, int limit) {
		return StringUtils.split(str, limit, " ");
	}

	/**
	 * @param str - the string to split
	 * @param delimiter - the delimiting regular expression
	 * @return A endless list all the elements resulting of {@code str} splitted using {@code delimiter} excluding empty results
	 */
	public static List<String> split(String str, String delimiter) {
		return StringUtils.split(str, -1, delimiter);
	}

	/**
	 * @param str - the string to split
	 * @return A list without limits containing all the elements resulting of {@code str} splitted using space excluding empty results
	 */
	public static List<String> split(String str) {
		return StringUtils.split(str, -1);
	}

	/**
	 * @param str - the string to capitalize
	 * @return The string capitalized
	 */
	public static String capitalize(String str) {
		switch (str.length()) {
			case 0:
			case 1:
				return str.toUpperCase();
			default:
				return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
		}
	}

	/**
	 * @param count - the number of elements
	 * @param str - the string to get plural from
	 * @return {@code String.format("%d %ss", count, str)} if count > 1, String.format("%d %s", count, str) otherwise
	 */
	public static String pluralOf(long count, String str) {
		if(count > 1) {
			return String.format("%d %ss", count, str);
		}
		return String.format("%d %s", count, str);
	}

	/**
	 * @param str - the string to truncate if necessary
	 * @param size - the maximum size of the string
	 * @return The string truncated to {@code size} characters with '...' at the end if its length is superior to {@code size}, str otherwise
	 */
	public static String truncate(String str, int size) {
		if(str.length() <= size) {
			return str;
		}

		String truncatedStr = str.substring(0, size - 3);
		return truncatedStr.substring(0, truncatedStr.lastIndexOf(' ')) + "...";
	}

	/**
	 * @param str - the string from which to remove patterns
	 * @param toRemove - the strings to be substituted for each match
	 * @return The resulting {@code String}
	 */
	public static String remove(String str, String... toRemove) {
		return str.replaceAll(Arrays.stream(toRemove)
				.filter(replacement -> !replacement.isEmpty())
				.map(Pattern::quote)
				.collect(Collectors.joining("|")), "");
	}

	/**
	 * @param str - the string from which to normalize spaces
	 * @return The string corresponding to {@code str} trimmed with every spaces replaced by a single one
	 */
	public static String normalizeSpace(String str) {
		return str.trim().replaceAll(" +", " ");
	}

	/**
	 * @param str - the string from which to count matches
	 * @param toMatch - the string to match
	 * @return The number of occurrences of {@code toMatch} in {@code str}
	 */
	public static int countMatches(String str, String toMatch) {
		return str.length() - str.replace(toMatch, "").length();
	}

	/**
	 * @param str - the string from which to extract words in quotation marks
	 * @return A {@link List} containing all the matched words or phrases in quotation marks
	 */
	public static List<String> getQuotedWords(String str) {
		List<String> matches = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(str);
		while(matcher.find()) {
			matches.add(matcher.group(1));
		}
		matches.removeAll(Collections.singleton(""));
		return matches;
	}

}
