package me.shadorc.shadbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

	public static List<String> split(String str, int limit, String delimiter) {
		return Arrays.stream(str.split(delimiter, limit))
				.map(word -> word.trim())
				.filter(word -> !word.isEmpty())
				.collect(Collectors.toList());
	}

	public static List<String> split(String str, int limit) {
		return StringUtils.split(str, limit, " ");
	}

	public static List<String> split(String str, String delimiter) {
		return StringUtils.split(str, -1, delimiter);
	}

	public static List<String> split(String str) {
		return StringUtils.split(str, -1);
	}

	public static String capitalize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
	}

	public static String singularOf(String str) {
		if(str.charAt(str.length() - 1) == 's') {
			return str.substring(0, str.length() - 1);
		}
		return str;
	}

	public static String pluralOf(long count, String str) {
		if(count > 1) {
			return String.format("%d %ss", count, str);
		}
		return String.format("%d %s", count, str);
	}

	public static String truncate(String str, int size) {
		if(str.length() > size) {
			return str.substring(0, size - 3) + "...";
		}
		return str;
	}

	public static String remove(String text, String... toRemove) {
		return text.replaceAll(Arrays.stream(toRemove).filter(str -> !str.isEmpty()).collect(Collectors.joining("|")), "");
	}

	public static String normalizeSpace(String str) {
		return str.trim().replaceAll(" +", " ");
	}

	public static List<String> getQuotedWords(String text) {
		List<String> matches = new ArrayList<>();
		Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(text);
		while(matcher.find()) {
			matches.add(matcher.group(1));
		}
		matches.removeAll(Collections.singleton(""));
		return matches;
	}

	public static int countMatches(String str, String toMatch) {
		return str.length() - str.replace(toMatch, "").length();
	}

}
