package com.shadorc.shadbot.utils;

import reactor.util.annotation.Nullable;

public final class NumberUtils {

    /**
     * @param str - the string to parse as an integer number, may be {@code null}
     * @return The string parsed as an Integer number or {@code null} if the string is not a valid representation of a number
     */
    @Nullable
    public static Integer toIntOrNull(@Nullable String str) {
        if (str == null) {
            return null;
        }

        try {
            return Integer.parseInt(str.trim());
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str - the string to parse as a positive integer number, may be {@code null}
     * @return The string parsed as a positive Integer number or {@code null} if the string is not a valid representation of a positive number
     */
    @Nullable
    public static Integer toPositiveIntOrNull(@Nullable String str) {
        final Integer value = NumberUtils.toIntOrNull(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to parse as an integer number between {@code min} and {@code max}, may be null
     * @param min - the minimum value, inclusive
     * @param max - the maximum value, inclusive
     * @return The string parsed as an integer or {@code null} if the string is not a valid representation of a positive integer
     * or is not between {@code min} and {@code max}
     */
    @Nullable
    public static Integer toIntBetweenOrNull(@Nullable String str, int min, int max) {
        final Integer value = NumberUtils.toIntOrNull(str);
        if (value == null || !NumberUtils.isBetween(value, min, max)) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to parse as a long number, may be {@code null}
     * @return The string parsed as a Long number or {@code null} if the string is not a valid representation of a number
     */
    @Nullable
    public static Long toLongOrNull(@Nullable String str) {
        if (str == null) {
            return null;
        }

        try {
            return Long.parseLong(str.trim());
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str - the string to parse as a positive long number, may be {@code null}
     * @return The string parsed as a positive Long number or {@code null} if the string is not a valid representation of a number
     */
    @Nullable
    public static Long toPositiveLongOrNull(@Nullable String str) {
        final Long value = NumberUtils.toLongOrNull(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to check, may be null
     * @return true if the string is a valid representation of a positive Long number, false otherwise
     */
    public static boolean isPositiveLong(@Nullable String str) {
        return NumberUtils.toPositiveLongOrNull(str) != null;
    }

    /**
     * @param num - the long to truncate between {@code min} and {@code max}
     * @param min - the minimum value, inclusive
     * @param max - the maximum value, inclusive
     * @return The long truncated between {@code min} and {@code max}
     */
    public static long truncateBetween(long num, long min, long max) {
        return Math.max(min, Math.min(num, max));
    }

    /**
     * @param num - the double to check
     * @param min - the minimum value, inclusive
     * @param max - the maximum value, inclusive
     * @return true if {@code num} is between {@code min} and {@code max}, false otherwise
     */
    public static boolean isBetween(double num, double min, double max) {
        return num >= min && num <= max;
    }

}
