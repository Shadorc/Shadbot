package me.shadorc.shadbot.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FormatUtils {

	public static <T> String formatList(List<T> list, Function<T, String> mapper, String delimiter) {
		return list.stream().map(mapper).collect(Collectors.joining(delimiter)).toString();
	}

	public static String formatArray(Object[] array, Function<Object, String> mapper, String delimiter) {
		return FormatUtils.formatList(Arrays.asList(array), mapper, delimiter);
	}

}
