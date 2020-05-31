package com.shadorc.shadbot.utils;

import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {

    private static final Pattern SPACES_PATTERN = Pattern.compile(" +");

    /**
     * @param str The string to capitalize, may be {@code null}.
     * @return The capitalized string.
     */
    @Nullable
    public static String capitalize(@Nullable String str) {
        if (str == null || str.isBlank()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * @param text The string to get quoted elements from.
     * @return A {@link List} containing the quoted elements from the provided text.
     */
    public static List<String> getQuotedElements(@Nullable String text) {
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
     * The function returns the argument string with whitespace normalized by using {@link String#trim()} to remove
     * leading and trailing whitespace and then replacing sequences of whitespace characters by a single space.
     *
     * @param str The source string to normalize whitespaces from, may be {@code null}.
     * @return The modified string with whitespace normalized or {@code null} if null string input.
     */
    @Nullable
    public static String normalizeSpace(@Nullable String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return SPACES_PATTERN.matcher(str.trim()).replaceAll(" ");
    }

    /**
     * @param count The number of elements.
     * @param str The string to get plural from, may be {@code null}.
     * @return {@code count str(s)} with {@code count} formatted using English locale.
     */
    @Nullable
    public static String pluralOf(long count, @Nullable String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        if (Math.abs(count) > 1) {
            return String.format("%s %ss", FormatUtils.number(count), str);
        }
        return String.format("%s %s", FormatUtils.number(count), str);
    }

    /**
     * @param str The string from which to remove patterns, may be {@code null}.
     * @param toRemove The strings to be substituted for each match.
     * @return The resulting string.
     */
    @Nullable
    public static String remove(@Nullable String str, @NonNull List<String> toRemove) {
        return StringUtils.remove(str, toRemove.toArray(new String[0]));
    }

    /**
     * @param str The string from which to remove patterns, may be {@code null}.
     * @param toRemove The strings to be substituted for each match.
     * @return The resulting string.
     */
    @Nullable
    public static String remove(@Nullable String str, @NonNull String... toRemove) {
        if (str == null) {
            return null;
        }
        return str.replaceAll(Arrays.stream(toRemove)
                .filter(replacement -> !replacement.isEmpty())
                .map(Pattern::quote)
                .collect(Collectors.joining("|")), "");
    }

    /**
     * @param str The string to split, may be {@code null}.
     * @return A list without limits containing all the elements resulting of {@code str} splitted using space excluding
     * empty results.
     */
    public static List<String> split(@Nullable String str) {
        return StringUtils.split(str, -1);
    }

    /**
     * @param str The string to split, may be {@code null}.
     * @param limit The result threshold.
     * @return An endless list containing all the elements resulting of {@code str} splitted using space excluding
     * empty results.
     */
    public static List<String> split(@Nullable String str, int limit) {
        return StringUtils.split(str, limit, " ");
    }

    /**
     * @param str The string to split, may be {@code null}.
     * @param limit The result threshold.
     * @param delimiter The delimiting regular expression.
     * @return A list with a maximum number of {@code limit} elements containing all the results of {@code str} splitted
     * using {@code delimiter} excluding empty results.
     */
    public static List<String> split(@Nullable String str, int limit, @NonNull String delimiter) {
        if (str == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(delimiter, limit))
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * @param str The string to split, may be {@code null}.
     * @param delimiter The delimiting regular expression.
     * @return An endless list all the elements resulting of {@code str} splitted using {@code delimiter} excluding
     * empty results.
     */
    public static List<String> split(@Nullable String str, @NonNull String delimiter) {
        return StringUtils.split(str, -1, delimiter);
    }

}
