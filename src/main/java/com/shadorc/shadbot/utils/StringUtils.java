package com.shadorc.shadbot.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

    /**
     * @param str - the string to capitalize, may be null
     * @return The capitalized string or null if null string input
     */
    public static String capitalize(String str) {
        if (str == null || str.isBlank()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * @param enumeration - the enumeration to format, may be null
     * @return The enumeration converted as a capitalized string with underscores replaced with spaces
     */
    public static <E extends Enum<E>> String capitalizeEnum(E enumeration) {
        if (enumeration == null) {
            return null;
        }
        return StringUtils.capitalize(enumeration.toString().toLowerCase().replace("_", " "));
    }

    /**
     * @param text - the string
     * @return A {@link List} containing the quoted elements from {@code text}
     */
    public static List<String> getQuotedElements(String text) {
        final List<String> matches = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return matches;
        }
        final Matcher matcher = Pattern.compile("\"([^\"]*)\"").matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group(1));
        }
        matches.removeAll(Collections.singleton(""));
        return matches;
    }

    /**
     * The function returns the argument string with whitespace normalized by using {@link String#trim()} to remove leading and trailing whitespace and
     * then replacing sequences of whitespace characters by a single space.
     *
     * @param str - the source string to normalize whitespaces from, may be null
     * @return the modified string with whitespace normalized or {@code null} if null string input
     */
    public static String normalizeSpace(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.trim().replaceAll(" +", " ");
    }

    /**
     * @param count - the number of elements
     * @param str   - the string to get plural from, may be null
     * @return {@code String.format("%d %ss", count, str)} if count > 1, String.format("%d %s", count, str) otherwise
     */
    public static String pluralOf(long count, String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        if (count > 1) {
            return String.format("%d %ss", count, str);
        }
        return String.format("%d %s", count, str);
    }

    /**
     * @param str      - the string from which to remove patterns, may be null
     * @param toRemove - the strings to be substituted for each match
     * @return The resulting string
     */
    public static String remove(String str, List<String> toRemove) {
        return StringUtils.remove(str, toRemove.toArray(new String[0]));
    }

    /**
     * @param str      - the string from which to remove patterns, may be null
     * @param toRemove - the strings to be substituted for each match
     * @return The resulting string
     */
    public static String remove(String str, String... toRemove) {
        if (str == null) {
            return null;
        }
        return str.replaceAll(Arrays.stream(toRemove)
                .filter(replacement -> !replacement.isEmpty())
                .map(Pattern::quote)
                .collect(Collectors.joining("|")), "");
    }

    /**
     * @param str - the string to split, may be null
     * @return A list without limits containing all the elements resulting of {@code str} splitted using space excluding empty results
     */
    public static List<String> split(String str) {
        return StringUtils.split(str, -1);
    }

    /**
     * @param str   - the string to split, may be null
     * @param limit - the result threshold
     * @return An endless list containing all the elements resulting of {@code str} splitted using space excluding empty results
     */
    public static List<String> split(String str, int limit) {
        return StringUtils.split(str, limit, " ");
    }

    /**
     * @param str       - the string to split, may be null
     * @param limit     - the result threshold
     * @param delimiter - the delimiting regular expression
     * @return A list with a maximum number of {@code limit} elements containing all the results of {@code str} splitted using {@code delimiter} excluding
     * empty results
     */
    public static List<String> split(String str, int limit, String delimiter) {
        if(str == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(delimiter, limit))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @param str       - the string to split
     * @param delimiter - the delimiting regular expression
     * @return A endless list all the elements resulting of {@code str} splitted using {@code delimiter} excluding empty results
     */
    public static List<String> split(String str, String delimiter) {
        return StringUtils.split(str, -1, delimiter);
    }

}
