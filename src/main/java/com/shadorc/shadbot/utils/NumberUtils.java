package com.shadorc.shadbot.utils;

public class NumberUtils {

    /**
     * @param str - the string to convert, may be null
     * @return The integer represented by the string or null if the string does not represent an integer
     */
    public static Integer asInt(String str) {
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
     * @param str - the string to convert, may be null
     * @return The positive integer represented by the string or null if the string does not represent a positive integer
     */
    public static Integer asPositiveInt(String str) {
        final Integer value = NumberUtils.asInt(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to convert, may be null
     * @param min - the minimum value, inclusive
     * @param max - the maximum value, inclusive
     * @return The integer represented by the string or null if the string does not represent a positive integer
     * or is not between min and max
     */
    public static Integer asIntBetween(String str, int min, int max) {
        final Integer value = NumberUtils.asInt(str);
        if (value == null || !NumberUtils.isBetween(value, min, max)) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to convert, may be null
     * @return The long represented by the string or null if the string does not represent a long
     */
    public static Long asLong(String str) {
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
     * @param str - the string to convert, may be null
     * @return The long represented by the string or null if the string does not represent a long
     */
    public static Long asPositiveLong(String str) {
        final Long value = NumberUtils.asLong(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str - the string to check, may be null
     * @return true if the string represents a positive long, false otherwise
     */
    public static boolean isPositiveLong(String str) {
        return NumberUtils.asPositiveLong(str) != null;
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
     * @return true if {@code num} is between {@code min}, inclusive, and {@code max}, inclusive, false otherwise
     */
    public static boolean isBetween(double num, double min, double max) {
        return num >= min && num <= max;
    }

}
