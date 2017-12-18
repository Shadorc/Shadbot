package me.shadorc.shadbot.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class StringUtils {

	public static List<String> split(String str, int limit, String delimiter) {
		return Arrays.stream(str.split(delimiter, limit)).filter(word -> word != null && !word.isEmpty()).collect(Collectors.toList());
	}

	public static List<String> split(String str, int limit) {
		return StringUtils.split(str, limit, " ");
	}

	public static List<String> split(String str) {
		return StringUtils.split(str, -1);
	}
}
