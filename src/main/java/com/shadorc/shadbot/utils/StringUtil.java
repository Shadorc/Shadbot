package com.shadorc.shadbot.utils;

import reactor.util.annotation.NonNull;
import reactor.util.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtil {

    private static final Pattern SPACES_PATTERN = Pattern.compile(" +");

    /**
     * @param str      The string to abbreviate, may be {@code null}.
     * @param maxWidth Maximum length of result string, must be at least 4.
     * @return Abbreviated string, {@code null} if null string input.
     * @throws IllegalArgumentException If the width is too small.
     */
    public static String abbreviate(@Nullable String str, int maxWidth) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxWidth) {
            return str;
        }
        final String abbrevMarker = "...";
        if (str.length() - abbrevMarker.length() <= 0) {
            throw new IllegalArgumentException("Minimum abbreviation width is %d".formatted(abbrevMarker.length() + 1));
        }
        return str.substring(0, maxWidth - abbrevMarker.length()) + abbrevMarker;
    }

    /**
     * @param str The string to capitalize, may be {@code null}.
     * @return The capitalized string.
     */
    @Nullable
    public static String capitalize(@Nullable String str) {
        if (StringUtil.isBlank(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
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
     * @param str      The string from which to remove patterns, may be {@code null}.
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
        return StringUtil.split(str, -1);
    }

    /**
     * @param str   The string to split, may be {@code null}.
     * @param limit The result threshold.
     * @return An endless list containing all the elements resulting of {@code str} splitted using space excluding
     * empty results.
     */
    public static List<String> split(@Nullable String str, int limit) {
        return StringUtil.split(str, limit, " ");
    }

    /**
     * @param str       The string to split, may be {@code null}.
     * @param limit     The result threshold.
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
     * @param str       The string to split, may be {@code null}.
     * @param delimiter The delimiting regular expression.
     * @return An endless list all the elements resulting of {@code str} splitted using {@code delimiter} excluding
     * empty results.
     */
    public static List<String> split(@Nullable String str, @NonNull String delimiter) {
        return StringUtil.split(str, -1, delimiter);
    }

    /**
     * @param str The string to check, may be {@code null}.
     * @return Whether the string is null or blank.
     */
    public static boolean isBlank(@Nullable String str) {
        return str == null || str.isBlank();
    }

}
